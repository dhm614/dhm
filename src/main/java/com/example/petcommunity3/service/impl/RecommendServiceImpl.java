package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.entity.UserBehavior;
import com.example.petcommunity3.entity.UserInterestTag;
import com.example.petcommunity3.entity.VideoStat;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.RecommendMapper;
import com.example.petcommunity3.mapper.UserBehaviorMapper;
import com.example.petcommunity3.mapper.UserInterestTagMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.RecommendConfigService;
import com.example.petcommunity3.service.RecommendScorer;
import com.example.petcommunity3.service.RecommendService;
import com.example.petcommunity3.service.RecommendWeights;
import com.example.petcommunity3.vo.SimilarScoreVO;
import com.example.petcommunity3.vo.VideoFeedItemVO;
import com.example.petcommunity3.vo.VideoFeedVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 多路召回 + 规则打分的短视频推荐实现。
 *
 * 链路：多路召回 → 过滤（状态/已看降权）→ 打分 → 多样性重排 → 分页 → 曝光埋点。
 * 打分细节见 {@link RecommendScorer}，权重与限额来自 recommend_config，可运行期调整。
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    private static final String REASON_HOT = "HOT";
    private static final String REASON_FOLLOW = "FOLLOW";
    private static final String REASON_TOPIC = "TOPIC";
    private static final String REASON_AUTHOR = "AUTHOR";
    private static final String REASON_SIMILAR = "SIMILAR";
    private static final String REASON_FRESH = "FRESH";
    private static final String REASON_LATEST = "LATEST";
    private static final String STRATEGY = "MULTI_RECALL_V1";

    /** 同一用户对同一视频的曝光去重窗口（分钟），避免刷新页面把曝光刷爆。 */
    private static final int IMPRESSION_DEDUP_MINUTES = 10;

    private final RecommendMapper recommendMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final UserBehaviorMapper behaviorMapper;
    private final UserInterestTagMapper interestTagMapper;
    private final RecommendScorer scorer;
    private final RecommendConfigService configService;

    private final Map<Long, CacheEntry> feedCache = new ConcurrentHashMap<>();

    public RecommendServiceImpl(RecommendMapper recommendMapper,
                                PostMapper postMapper,
                                UserMapper userMapper,
                                UserBehaviorMapper behaviorMapper,
                                UserInterestTagMapper interestTagMapper,
                                RecommendScorer scorer,
                                RecommendConfigService configService) {
        this.recommendMapper = recommendMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.behaviorMapper = behaviorMapper;
        this.interestTagMapper = interestTagMapper;
        this.scorer = scorer;
        this.configService = configService;
    }

    @Override
    public VideoFeedVO recommend(Long userId, int limit, int offset, String traceId) {
        RecommendWeights weights = RecommendWeights.load(configService);
        int safeLimit = Math.max(1, Math.min(limit, 20));
        int safeOffset = Math.max(0, offset);
        String trace = StringUtils.hasText(traceId)
                ? traceId : UUID.randomUUID().toString().replace("-", "");

        List<RankedItem> ranked = getRankedCandidates(userId, weights);
        List<RankedItem> ordered = diversify(ranked, weights);

        int from = Math.min(safeOffset, ordered.size());
        int to = Math.min(from + safeLimit, ordered.size());
        List<RankedItem> page = new ArrayList<>(ordered.subList(from, to));

        VideoFeedVO vo = new VideoFeedVO();
        vo.setItems(toItems(page));
        vo.setTraceId(trace);
        vo.setStrategy(STRATEGY);
        vo.setOffset(safeOffset);
        vo.setHasMore(to < ordered.size());

        recordImpressions(userId, page, trace);
        return vo;
    }

    @Override
    public void invalidateCache(Long userId) {
        if (userId == null) {
            feedCache.remove(0L);
        } else {
            feedCache.remove(userId);
        }
    }

    @Override
    public List<Long> rankVideoIds(Long userId, Collection<Long> excludeIds, int limit) {
        RecommendWeights weights = RecommendWeights.load(configService);
        List<RankedItem> ranked = buildCandidates(userId, weights, true);
        if (excludeIds != null && !excludeIds.isEmpty()) {
            Set<Long> exclude = new HashSet<>(excludeIds);
            ranked.removeIf(item -> item.post.getId() != null && exclude.contains(item.post.getId()));
        }
        List<RankedItem> ordered = diversify(ranked, weights);
        return ordered.stream()
                .limit(Math.max(1, limit))
                .map(item -> item.post.getId())
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> rankVideoIdsForEvaluation(Long userId, int limit) {
        RecommendWeights weights = RecommendWeights.load(configService);
        List<RankedItem> ranked = buildCandidates(userId, weights, false);
        List<RankedItem> ordered = diversify(ranked, weights);
        return ordered.stream()
                .limit(Math.max(1, limit))
                .map(item -> item.post.getId())
                .collect(Collectors.toList());
    }

    private List<RankedItem> getRankedCandidates(Long userId, RecommendWeights weights) {
        long key = userId == null ? 0L : userId;
        CacheEntry cached = feedCache.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expireAt > now) {
            return cached.items;
        }
        List<RankedItem> items = buildCandidates(userId, weights, true);
        feedCache.put(key, new CacheEntry(items, now + weights.feedCacheTtlMs));
        return items;
    }

    private List<RankedItem> buildCandidates(Long userId, RecommendWeights weights, boolean applySeenPenalty) {
        int maxCandidates = weights.maxCandidates;
        Map<Long, String> reasonByVideo = new LinkedHashMap<>();
        Set<Long> seenVideoIds = new HashSet<>();

        if (userId != null) {
            seenVideoIds.addAll(recommendMapper.selectRecentPositiveVideoIds(userId));
            merge(reasonByVideo, recommendMapper.selectFollow(userId, maxCandidates), REASON_FOLLOW);
            merge(reasonByVideo, recommendMapper.selectByTopicInterest(userId, maxCandidates), REASON_TOPIC);
            merge(reasonByVideo, recommendMapper.selectByAuthorInterest(userId, maxCandidates), REASON_AUTHOR);
            if (!seenVideoIds.isEmpty()) {
                merge(reasonByVideo,
                        recommendMapper.selectSimilar(new ArrayList<>(seenVideoIds), maxCandidates),
                        REASON_SIMILAR);
            }
        }
        merge(reasonByVideo, recommendMapper.selectHot(maxCandidates), REASON_HOT);
        merge(reasonByVideo, recommendMapper.selectFresh(maxCandidates), REASON_FRESH);
        if (reasonByVideo.size() < maxCandidates) {
            merge(reasonByVideo, recommendMapper.selectLatest(maxCandidates), REASON_LATEST);
        }

        if (reasonByVideo.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> videoIds = new ArrayList<>(reasonByVideo.keySet());
        Map<Long, VideoStat> statMap = loadStats(videoIds);
        Map<Long, Post> postMap = loadPosts(videoIds);
        Map<String, Double> topicWeights = new HashMap<>();
        Map<String, Double> authorWeights = new HashMap<>();
        if (userId != null) {
            loadInterestWeights(userId, topicWeights, authorWeights);
        }

        double maxEngagement = videoIds.stream()
                .mapToDouble(id -> scorer.weightedEngagement(statMap.get(id)))
                .max()
                .orElse(0d);
        LocalDateTime now = LocalDateTime.now();

        Map<Long, Double> similarityMap = new HashMap<>();
        if (userId != null) {
            similarityMap = recommendMapper.selectSimilarityScores(userId, videoIds).stream()
                    .filter(item -> item.getVideoId() != null && item.getScore() != null)
                    .collect(Collectors.toMap(SimilarScoreVO::getVideoId, SimilarScoreVO::getScore, Math::max));
        }

        List<RankedItem> items = new ArrayList<>();
        for (Map.Entry<Long, String> entry : reasonByVideo.entrySet()) {
            Post post = postMap.get(entry.getKey());
            if (post == null) {
                continue;
            }
            VideoStat stat = statMap.getOrDefault(entry.getKey(), VideoStat.empty(entry.getKey()));
            double interest = scorer.interestMatch(post, topicWeights, authorWeights);
            double similarity = similarityMap.getOrDefault(post.getId(), 0d);
            double score = scorer.computeScore(stat, post, interest, similarity, maxEngagement, now, weights);
            items.add(new RankedItem(post, stat, entry.getValue(), score,
                    seenVideoIds.contains(post.getId())));
        }
        // 已看内容不硬过滤，只打折降权，避免信息流被“看完了”卡死
        items.sort((a, b) -> Double.compare(
                orderScore(b, weights, applySeenPenalty),
                orderScore(a, weights, applySeenPenalty)));
        return items;
    }

    private static double orderScore(RankedItem item, RecommendWeights weights, boolean applySeenPenalty) {
        return item.seen && applySeenPenalty ? item.score * weights.seenPenalty : item.score;
    }

    private Map<Long, VideoStat> loadStats(List<Long> videoIds) {
        return recommendMapper.selectStatsByIds(videoIds).stream()
                .collect(Collectors.toMap(VideoStat::getVideoId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, Post> loadPosts(List<Long> videoIds) {
        if (videoIds.isEmpty()) {
            return new HashMap<>();
        }
        return postMapper.selectBatchIds(videoIds).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity(), (a, b) -> a));
    }

    private void loadInterestWeights(Long userId,
                                     Map<String, Double> topicWeights,
                                     Map<String, Double> authorWeights) {
        List<UserInterestTag> tags = interestTagMapper.selectList(
                new LambdaQueryWrapper<UserInterestTag>().eq(UserInterestTag::getUserId, userId));
        for (UserInterestTag tag : tags) {
            double weight = tag.getWeight() == null ? 0d : tag.getWeight().doubleValue();
            if (UserInterestTag.TYPE_TOPIC.equals(tag.getTagType())) {
                topicWeights.merge(tag.getTagValue(), weight, Math::max);
            } else if (UserInterestTag.TYPE_AUTHOR.equals(tag.getTagType())) {
                authorWeights.merge(tag.getTagValue(), weight, Math::max);
            }
        }
    }

    /**
     * 多样性重排：同作者、同话题都有上限；被挤出的内容保留到尾部兜底。
     */
    private List<RankedItem> diversify(List<RankedItem> ranked, RecommendWeights weights) {
        List<RankedItem> primary = new ArrayList<>();
        List<RankedItem> overflow = new ArrayList<>();
        Map<Long, Integer> authorCount = new HashMap<>();
        Map<String, Integer> topicCount = new HashMap<>();

        for (RankedItem item : ranked) {
            Long authorId = item.post.getUserId();
            String topic = item.post.getTopic() == null ? "" : item.post.getTopic();
            boolean authorFull = authorId != null
                    && authorCount.getOrDefault(authorId, 0) >= weights.maxPerAuthor;
            boolean topicFull = !topic.isEmpty()
                    && topicCount.getOrDefault(topic, 0) >= weights.maxPerTopic;
            if (authorFull || topicFull) {
                overflow.add(item);
                continue;
            }
            primary.add(item);
            if (authorId != null) {
                authorCount.merge(authorId, 1, Integer::sum);
            }
            if (!topic.isEmpty()) {
                topicCount.merge(topic, 1, Integer::sum);
            }
        }
        primary.addAll(overflow);
        return primary;
    }

    private List<VideoFeedItemVO> toItems(List<RankedItem> page) {
        if (page.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> authorIds = page.stream()
                .map(item -> item.post.getUserId())
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> authors = authorIds.isEmpty() ? new HashMap<>()
                : userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        List<VideoFeedItemVO> items = new ArrayList<>(page.size());
        for (RankedItem item : page) {
            VideoFeedItemVO vo = new VideoFeedItemVO();
            vo.setId(item.post.getId());
            vo.setTitle(item.post.getTitle());
            vo.setContent(item.post.getContent());
            vo.setCover(item.post.getCover());
            vo.setVideoUrl(item.post.getVideoUrl());
            vo.setTopic(item.post.getTopic());
            vo.setAuthorId(item.post.getUserId());
            User author = item.post.getUserId() == null ? null : authors.get(item.post.getUserId());
            if (author != null) {
                vo.setAuthorName(StringUtils.hasText(author.getNickname())
                        ? author.getNickname() : author.getUsername());
                vo.setAuthorAvatar(author.getAvatar());
            }
            vo.setLikeCount(item.post.getLikeCount());
            vo.setCommentCount(item.post.getCommentCount());
            vo.setCollectCount(item.post.getCollectCount());
            vo.setCreateTime(item.post.getCreateTime());
            vo.setHotScore(item.stat.getHotScore() == null ? 0d : item.stat.getHotScore().doubleValue());
            vo.setReason(item.reason);
            items.add(vo);
        }
        return items;
    }

    /**
     * 曝光埋点：同一用户对同一视频在去重窗口内只记一次，防止刷新刷量。
     */
    private void recordImpressions(Long userId, List<RankedItem> page, String traceId) {
        if (userId == null || page.isEmpty()) {
            return;
        }
        List<Long> videoIds = page.stream()
                .map(item -> item.post.getId())
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        if (videoIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Set<Long> recent = new HashSet<>(recommendMapper.selectRecentImpressionTargets(
                userId, videoIds, now.minusMinutes(IMPRESSION_DEDUP_MINUTES)));

        List<UserBehavior> behaviors = new ArrayList<>(videoIds.size());
        for (Long videoId : videoIds) {
            if (recent.contains(videoId)) {
                continue;
            }
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(userId);
            behavior.setTargetType("VIDEO");
            behavior.setTargetId(videoId);
            behavior.setAction(UserBehavior.ACTION_IMPRESSION);
            behavior.setWatchMs(0);
            behavior.setWatchRatio(BigDecimal.ZERO);
            behavior.setScene("FEED");
            behavior.setTraceId(traceId);
            behavior.setCreateTime(now);
            behaviors.add(behavior);
        }
        if (!behaviors.isEmpty()) {
            behaviorMapper.insertBatch(behaviors);
        }
    }

    private void merge(Map<Long, String> reasonByVideo, List<Post> posts, String reason) {
        if (posts == null) {
            return;
        }
        for (Post post : posts) {
            if (post != null && post.getId() != null) {
                reasonByVideo.putIfAbsent(post.getId(), reason);
            }
        }
    }

    private static final class RankedItem {
        private final Post post;
        private final VideoStat stat;
        private final String reason;
        private final double score;
        private final boolean seen;

        private RankedItem(Post post, VideoStat stat, String reason, double score, boolean seen) {
            this.post = post;
            this.stat = stat;
            this.reason = reason;
            this.score = score;
            this.seen = seen;
        }
    }

    private static final class CacheEntry {
        private final List<RankedItem> items;
        private final long expireAt;

        private CacheEntry(List<RankedItem> items, long expireAt) {
            this.items = items;
            this.expireAt = expireAt;
        }
    }
}
