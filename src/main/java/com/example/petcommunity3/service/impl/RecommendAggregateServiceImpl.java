package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.UserBehavior;
import com.example.petcommunity3.entity.UserInterestTag;
import com.example.petcommunity3.entity.VideoSimilar;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.RecommendMapper;
import com.example.petcommunity3.mapper.UserBehaviorMapper;
import com.example.petcommunity3.mapper.UserInterestTagMapper;
import com.example.petcommunity3.mapper.VideoSimilarMapper;
import com.example.petcommunity3.service.RecommendAggregateService;
import com.example.petcommunity3.vo.AggregateResultVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 离线聚合：行为流水 → 视频热度、用户兴趣标签、相似视频。
 * 全部在应用层用一次扫描完成，数据规模在单机可接受范围内。
 */
@Service
public class RecommendAggregateServiceImpl implements RecommendAggregateService {
    private static final int BEHAVIOR_WINDOW_DAYS = 30;
    private static final int SIMILAR_TOP_K = 10;
    private static final int MAX_VIDEOS_PER_USER = 50;
    private static final int BATCH_SIZE = 500;
    private static final double MIN_SIMILAR_SCORE = 0.05d;
    private static final double MAX_TAG_WEIGHT = 10d;

    private static final Set<String> POSITIVE_ACTIONS = Set.of(
            UserBehavior.ACTION_PLAY_END,
            UserBehavior.ACTION_LIKE,
            UserBehavior.ACTION_COMMENT,
            UserBehavior.ACTION_COLLECT,
            UserBehavior.ACTION_SHARE
    );

    private static final Map<String, Double> ACTION_WEIGHT = Map.of(
            UserBehavior.ACTION_PLAY_END, 1.0d,
            UserBehavior.ACTION_LIKE, 2.0d,
            UserBehavior.ACTION_COMMENT, 2.5d,
            UserBehavior.ACTION_COLLECT, 3.0d,
            UserBehavior.ACTION_SHARE, 3.0d
    );

    private final RecommendMapper recommendMapper;
    private final UserBehaviorMapper behaviorMapper;
    private final PostMapper postMapper;
    private final UserInterestTagMapper interestTagMapper;
    private final VideoSimilarMapper videoSimilarMapper;

    public RecommendAggregateServiceImpl(RecommendMapper recommendMapper,
                                         UserBehaviorMapper behaviorMapper,
                                         PostMapper postMapper,
                                         UserInterestTagMapper interestTagMapper,
                                         VideoSimilarMapper videoSimilarMapper) {
        this.recommendMapper = recommendMapper;
        this.behaviorMapper = behaviorMapper;
        this.postMapper = postMapper;
        this.interestTagMapper = interestTagMapper;
        this.videoSimilarMapper = videoSimilarMapper;
    }

    @Override
    public AggregateResultVO refreshAll() {
        long start = System.currentTimeMillis();
        AggregateResultVO result = new AggregateResultVO();
        result.setStatRows(recommendMapper.refreshVideoStat());
        result.setTagRows(rebuildInterestTags());
        result.setSimilarRows(rebuildSimilarVideos());
        recommendMapper.deleteOldBehaviors();
        result.setCostMs(System.currentTimeMillis() - start);
        result.setFinishedAt(LocalDateTime.now());
        return result;
    }

    private List<UserBehavior> loadRecentPositiveBehaviors() {
        return behaviorMapper.selectList(new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getTargetType, "VIDEO")
                .in(UserBehavior::getAction, POSITIVE_ACTIONS)
                .ge(UserBehavior::getCreateTime, LocalDateTime.now().minusDays(BEHAVIOR_WINDOW_DAYS)));
    }

    private int rebuildInterestTags() {
        List<UserBehavior> behaviors = loadRecentPositiveBehaviors();
        if (behaviors.isEmpty()) {
            interestTagMapper.deleteAll();
            return 0;
        }

        Set<Long> videoIds = behaviors.stream()
                .map(UserBehavior::getTargetId)
                .collect(Collectors.toSet());
        Map<Long, Post> postMap = postMapper.selectBatchIds(videoIds).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity(), (a, b) -> a));

        Map<Long, Map<String, Double>> accumulator = new HashMap<>();
        for (UserBehavior behavior : behaviors) {
            Post post = postMap.get(behavior.getTargetId());
            if (post == null) {
                continue;
            }
            double weight = ACTION_WEIGHT.getOrDefault(behavior.getAction(), 1.0d);
            if (UserBehavior.ACTION_PLAY_END.equals(behavior.getAction()) && behavior.getWatchRatio() != null) {
                weight *= 0.5d + behavior.getWatchRatio().doubleValue();
            }
            if (StringUtils.hasText(post.getTopic())) {
                accumulate(accumulator, behavior.getUserId(), UserInterestTag.TYPE_TOPIC, post.getTopic(), weight);
            }
            if (post.getUserId() != null) {
                accumulate(accumulator, behavior.getUserId(), UserInterestTag.TYPE_AUTHOR,
                        String.valueOf(post.getUserId()), weight);
            }
        }

        List<UserInterestTag> tags = new ArrayList<>();
        for (Map.Entry<Long, Map<String, Double>> userEntry : accumulator.entrySet()) {
            Map<String, Double> tagWeights = userEntry.getValue();
            double max = tagWeights.values().stream().max(Double::compareTo).orElse(1d);
            if (max <= 0) {
                continue;
            }
            for (Map.Entry<String, Double> tagEntry : tagWeights.entrySet()) {
                String[] parts = tagEntry.getKey().split("\u0001", 2);
                double normalized = MAX_TAG_WEIGHT * tagEntry.getValue() / max;
                if (normalized < 0.05d) {
                    continue;
                }
                UserInterestTag tag = new UserInterestTag();
                tag.setUserId(userEntry.getKey());
                tag.setTagType(parts[0]);
                tag.setTagValue(parts[1]);
                tag.setWeight(BigDecimal.valueOf(normalized).setScale(4, RoundingMode.HALF_UP));
                tags.add(tag);
            }
        }

        interestTagMapper.deleteAll();
        int inserted = 0;
        for (int i = 0; i < tags.size(); i += BATCH_SIZE) {
            List<UserInterestTag> batch = tags.subList(i, Math.min(i + BATCH_SIZE, tags.size()));
            if (!batch.isEmpty()) {
                inserted += interestTagMapper.insertBatch(batch);
            }
        }
        return inserted;
    }

    private void accumulate(Map<Long, Map<String, Double>> accumulator,
                            Long userId, String type, String value, double weight) {
        if (userId == null || value == null) {
            return;
        }
        accumulator.computeIfAbsent(userId, k -> new HashMap<>())
                .merge(type + "\u0001" + value, weight, Double::sum);
    }

    private int rebuildSimilarVideos() {
        List<UserBehavior> behaviors = loadRecentPositiveBehaviors();
        if (behaviors.isEmpty()) {
            videoSimilarMapper.deleteAll();
            return 0;
        }

        Map<Long, Set<Long>> userToVideos = new HashMap<>();
        for (UserBehavior behavior : behaviors) {
            userToVideos.computeIfAbsent(behavior.getUserId(), k -> new LinkedHashSet<>())
                    .add(behavior.getTargetId());
        }

        Map<Long, Integer> videoDegree = new HashMap<>();
        Map<Long, Map<Long, Integer>> coOccurrence = new HashMap<>();
        for (Set<Long> videos : userToVideos.values()) {
            List<Long> list = new ArrayList<>(videos);
            if (list.size() > MAX_VIDEOS_PER_USER) {
                list = list.subList(0, MAX_VIDEOS_PER_USER);
            }
            for (Long videoId : list) {
                videoDegree.merge(videoId, 1, Integer::sum);
            }
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    Long a = list.get(i);
                    Long b = list.get(j);
                    increment(coOccurrence, a, b);
                    increment(coOccurrence, b, a);
                }
            }
        }

        List<VideoSimilar> rows = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Integer>> entry : coOccurrence.entrySet()) {
            Long sourceId = entry.getKey();
            int sourceDegree = videoDegree.getOrDefault(sourceId, 1);
            entry.getValue().entrySet().stream()
                    .sorted(Comparator.<Map.Entry<Long, Integer>>comparingInt(Map.Entry::getValue).reversed())
                    .limit(SIMILAR_TOP_K)
                    .forEach(pair -> {
                        int targetDegree = videoDegree.getOrDefault(pair.getKey(), 1);
                        double score = pair.getValue() / Math.sqrt((double) sourceDegree * targetDegree);
                        if (score < MIN_SIMILAR_SCORE) {
                            return;
                        }
                        VideoSimilar similar = new VideoSimilar();
                        similar.setVideoId(sourceId);
                        similar.setSimilarVideoId(pair.getKey());
                        similar.setScore(BigDecimal.valueOf(score).setScale(6, RoundingMode.HALF_UP));
                        rows.add(similar);
                    });
        }

        videoSimilarMapper.deleteAll();
        int inserted = 0;
        for (int i = 0; i < rows.size(); i += BATCH_SIZE) {
            List<VideoSimilar> batch = rows.subList(i, Math.min(i + BATCH_SIZE, rows.size()));
            if (!batch.isEmpty()) {
                inserted += videoSimilarMapper.insertBatch(batch);
            }
        }
        return inserted;
    }

    private void increment(Map<Long, Map<Long, Integer>> coOccurrence, Long from, Long to) {
        coOccurrence.computeIfAbsent(from, k -> new HashMap<>()).merge(to, 1, Integer::sum);
    }
}
