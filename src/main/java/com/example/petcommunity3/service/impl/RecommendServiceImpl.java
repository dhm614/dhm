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
import com.example.petcommunity3.service.RecommendService;
import com.example.petcommunity3.vo.VideoFeedItemVO;
import com.example.petcommunity3.vo.VideoFeedVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * 打分口径（权重之和为 1）：
 * 0.30 质量 Wilson 下界   —— 用完播数/播放数衡量内容是否真的让人看完
 * 0.15 完播完整度         —— 平均观看比例，平滑冷启动
 * 0.15 互动强度           —— 点赞/评论/收藏/分享的对数归一化
 * 0.15 新鲜度             —— 72 小时半衰
 * 0.25 兴趣匹配           —— 话题/作者兴趣标签权重
 */
@Service
public class RecommendServiceImpl implements RecommendService {
    private static final int CACHE_TTL_MS = 120_000;
    private static final int MAX_CANDIDATES = 60;
    private static final double WILSON_Z = 1.96d;
    private static final int MAX_PER_AUTHOR = 2;
    private static final int MAX_PER_TOPIC = 3;
    /** 已看过的内容打分惩罚系数，避免重复占位但不做硬过滤 */
    private static final double SEEN_PENALTY = 0.65d;
    private static final double HALF_LIFE_HOURS = 72d;

    private static final String REASON_HOT = "HOT";
    private static final String REASON_FOLLOW = "FOLLOW";
    private static final String REASON_TOPIC = "TOPIC";
    private static final String REASON_AUTHOR = "AUTHOR";
    private static final String REASON_SIMILAR = "SIMILAR";
    private static final String REASON_FRESH = "FRESH";
    private static final String REASON_LATEST = "LATEST";

    private final RecommendMapper recommendMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final UserBehaviorMapper behaviorMapper;
    private final UserInterestTagMapper interestTagMapper;

    private final Map<Long, CacheEntry> feedCache = new ConcurrentHashMap<>();

    public RecommendServiceImpl(RecommendMapper recommendMapper,
                                PostMapper postMapper,
                                UserMapper userMapper,
                                UserBehaviorMapper behaviorMapper,
                                UserInterestTagMapper interestTagMapper) {
        this.recommendMapper = recommendMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.behaviorMapper = behaviorMapper;
        this.interestTagMapper = interestTagMapper;
    }

    @Override
    public VideoFeedVO recommend(Long userId, int limit, int offset, String traceId) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        int safeOffset = Math.max(0, offset);
        String trace = StringUtils.hasText(traceId) ? traceId : UUID.randomUUID().toString().replace("-", "");

        List<RankedItem> ranked = getRankedCandidates(userId);
        List<RankedItem> ordered = diversify(ranked);

        int from = Math.min(safeOffset, ordered.size());
        int to = Math.min(from + safeLimit, ordered.size());
        List<RankedItem> page = new ArrayList<>(ordered.subList(from, to));

        VideoFeedVO vo = new VideoFeedVO();
        vo.setItems(toItems(page));
        vo.setTraceId(trace);
        vo.setStrategy("MULTI_RECALL_V1");
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

    private List<RankedItem> getRankedCandidates(Long userId) {
        long key = userId == null ? 0L : userId;
        CacheEntry cached = feedCache.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expireAt > now) {
            return cached.items;
        }
        List<RankedItem> items = buildCandidates(userId);
        feedCache.put(key, new CacheEntry(items, now + CACHE_TTL_MS));
        return items;
    }

    private List<RankedItem> buildCandidates(Long userId) {
        Map<Long, String> reasonByVideo = new LinkedHashMap<>();
        Set<Long> seenVideoIds = new HashSet<>();

        if (userId != null) {
            seenVideoIds.addAll(recommendMapper.selectRecentPositiveVideoIds(userId));
            merge(reasonByVideo, recommendMapper.selectFollow(userId, MAX_CANDIDATES), REASON_FOLLOW);
            merge(reasonByVideo, recommendMapper.selectByTopicInterest(userId, MAX_CANDIDATES), REASON_TOPIC);
            merge(reasonByVideo, recommendMapper.selectByAuthorInterest(userId, MAX_CANDIDATES), REASON_AUTHOR);
            if (!seenVideoIds.isEmpty()) {
                merge(reasonByVideo,
                        recommendMapper.selectSimilar(new ArrayList<>(seenVideoIds), MAX_CANDIDATES),
                        REASON_SIMILAR);
            }
        }
        merge(reasonByVideo, recommendMapper.selectHot(MAX_CANDIDATES), REASON_HOT);
        merge(reasonByVideo, recommendMapper.selectFresh(MAX_CANDIDATES), REASON_FRESH);
        if (reasonByVideo.size() < MAX_CANDIDATES) {
            merge(reasonByVideo, recommendMapper.selectLatest(MAX_CANDIDATES), REASON_LATEST);
        }

        // 已消费内容不剔除，只做降级：未看过的优先，看过的兜底，避免频繁重复
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
                .mapToDouble(id -> weightedEngagement(statMap.get(id)))
                .max()
                .orElse(0d);
        LocalDateTime now = LocalDateTime.now();

        List<RankedItem> items = new ArrayList<>();
        for (Map.Entry<Long, String> entry : reasonByVideo.entrySet()) {
            Post post = postMap.get(entry.getKey());
            if (post == null) {
                continue;
            }
            VideoStat stat = statMap.getOrDefault(entry.getKey(), VideoStat.empty(entry.getKey()));
            double interest = interestMatch(post, topicWeights, authorWeights);
            double score = computeScore(stat, post, interest, maxEngagement, now);
            items.add(new RankedItem(post, stat, entry.getValue(), score, seenVideoIds.contains(post.getId())));
        }
        items.sort((a, b) -> Double.compare(
                b.seen ? b.score * SEEN_PENALTY : b.score,
                a.seen ? a.score * SEEN_PENALTY : a.score));
        return items;
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

    private double interestMatch(Post post, Map<String, Double> topicWeights, Map<String, Double> authorWeights) {
        double topic = 0d;
        if (StringUtils.hasText(post.getTopic())) {
            topic = topicWeights.getOrDefault(post.getTopic(), 0d) / 10d;
        }
        double author = post.getUserId() == null ? 0d
                : authorWeights.getOrDefault(String.valueOf(post.getUserId()), 0d) / 10d;
        return Math.min(1d, 0.7d * topic + 0.3d * author);
    }

    private double weightedEngagement(VideoStat stat) {
        if (stat == null) {
            return 0d;
        }
        return safe(stat.getLikeCnt()) * 1.0d
                + safe(stat.getCommentCnt()) * 2.0d
                + safe(stat.getCollectCnt()) * 2.0d
                + safe(stat.getShareCnt()) * 3.0d;
    }

    private double computeScore(VideoStat stat, Post post, double interest,
                                double maxEngagement, LocalDateTime now) {
        int plays = safe(stat.getPlayCnt());
        int finishes = safe(stat.getPlayEndCnt());
        double quality = wilsonLowerBound(finishes, plays, WILSON_Z);

        double completeness = stat.getAvgWatchRatio() == null ? 0d : stat.getAvgWatchRatio().doubleValue();
        completeness = Math.min(1d, Math.max(0d, completeness));

        double engagement = maxEngagement <= 0d ? 0d
                : Math.log1p(weightedEngagement(stat)) / Math.log1p(maxEngagement);

        double freshness = 0d;
        if (post.getCreateTime() != null) {
            double hours = Math.max(0d, Duration.between(post.getCreateTime(), now).toMinutes() / 60d);
            freshness = Math.pow(0.5d, hours / HALF_LIFE_HOURS);
        }

        return 0.18d * quality
                + 0.05d * completeness
                + 0.05d * engagement
                + 0.07d * freshness
                + 0.65d * interest;
    }

    private double wilsonLowerBound(int positives, int total, double z) {
        if (total <= 0) {
            return 0d;
        }
        double p = (double) positives / total;
        double denominator = 1d + z * z / total;
        double center = p + z * z / (2d * total);
        double margin = z * Math.sqrt(p * (1d - p) / total + z * z / (4d * total * total));
        return Math.max(0d, (center - margin) / denominator);
    }

    /**
     * 多样性重排：同作者最多 2 条、同话题最多 3 条；被挤出的内容保留到尾部兜底。
     */
    private List<RankedItem> diversify(List<RankedItem> ranked) {
        List<RankedItem> primary = new ArrayList<>();
        List<RankedItem> overflow = new ArrayList<>();
        Map<Long, Integer> authorCount = new HashMap<>();
        Map<String, Integer> topicCount = new HashMap<>();

        for (RankedItem item : ranked) {
            Long authorId = item.post.getUserId();
            String topic = item.post.getTopic() == null ? "" : item.post.getTopic();
            boolean authorFull = authorId != null && authorCount.getOrDefault(authorId, 0) >= MAX_PER_AUTHOR;
            boolean topicFull = !topic.isEmpty() && topicCount.getOrDefault(topic, 0) >= MAX_PER_TOPIC;
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
                vo.setAuthorName(StringUtils.hasText(author.getNickname()) ? author.getNickname() : author.getUsername());
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

    private void recordImpressions(Long userId, List<RankedItem> page, String traceId) {
        // 匿名用户没有画像，曝光记录没有下游价值，直接跳过
        if (userId == null || page.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<UserBehavior> behaviors = new ArrayList<>(page.size());
        for (RankedItem item : page) {
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(userId == null ? 0L : userId);
            behavior.setTargetType("VIDEO");
            behavior.setTargetId(item.post.getId());
            behavior.setAction(UserBehavior.ACTION_IMPRESSION);
            behavior.setWatchMs(0);
            behavior.setWatchRatio(BigDecimal.ZERO);
            behavior.setScene("FEED");
            behavior.setTraceId(traceId);
            behavior.setCreateTime(now);
            behaviors.add(behavior);
        }
        behaviorMapper.insertBatch(behaviors);
    }

    private void merge(Map<Long, String> reasonByVideo, List<Post> posts, String reason) {
        for (Post post : posts) {
            if (post != null && post.getId() != null) {
                reasonByVideo.putIfAbsent(post.getId(), reason);
            }
        }
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
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
