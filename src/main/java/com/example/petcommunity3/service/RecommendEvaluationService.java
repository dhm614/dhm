package com.example.petcommunity3.service;

import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.mapper.RecommendMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 离线评估：leave-one-out。
 *
 * 对每个正反馈足够多的用户，取他最近一次正反馈的视频作为 holdout，
 * 用当前画像与召回逻辑给全站排序，看 holdout 落在第几名：
 *   HR@K   = 命中用户数 / 参与评估用户数
 *   NDCG@K = 命中时 1 / log2(rank + 1)（只有一个相关项，理想 DCG 为 1）
 *
 * 对照组是同一批用户的「纯热门 TopK」与「纯最新 TopK」，随机基线取 K / 视频总数。
 * 注意：画像标签由全量行为聚合而来，严格来说含有 holdout 自身的信息（时间泄漏），
 * 因此该指标用于版本间横向对比，不宜当作线上效果的绝对估计。
 */
@Service
public class RecommendEvaluationService {
    private static final int MIN_INTERACTIONS = 3;
    private static final int MAX_DETAIL_ROWS = 20;

    private final RecommendMapper recommendMapper;
    private final RecommendService recommendService;

    public RecommendEvaluationService(RecommendMapper recommendMapper, RecommendService recommendService) {
        this.recommendMapper = recommendMapper;
        this.recommendService = recommendService;
    }

    public Map<String, Object> evaluate(int topK, int maxUsers) {
        int k = Math.max(1, Math.min(topK, 50));
        int userLimit = Math.max(1, Math.min(maxUsers, 500));

        List<Long> userIds = recommendMapper.selectEvaluableUserIds(MIN_INTERACTIONS, userLimit);
        List<Post> hotTop = recommendMapper.selectHot(k + 1);
        List<Post> latestTop = recommendMapper.selectLatest(k + 1);
        List<Long> hotIds = hotTop.stream().map(Post::getId).limit(k).toList();
        List<Long> latestIds = latestTop.stream().map(Post::getId).limit(k).toList();
        int totalVideos = recommendMapper.countPublicVideos();

        int evaluated = 0;
        int hits = 0;
        int hotHits = 0;
        int latestHits = 0;
        double ndcgSum = 0d;
        List<Map<String, Object>> details = new ArrayList<>();
        Map<Long, List<Long>> rankedByUser = new LinkedHashMap<>();
        Map<Long, String> mainTopicByUser = new LinkedHashMap<>();

        for (Long userId : userIds) {
            Long holdout = recommendMapper.selectLatestPositiveTarget(userId);
            if (holdout == null) {
                continue;
            }
            evaluated++;
            List<Long> ranked = recommendService.rankVideoIdsForEvaluation(userId, k);
            rankedByUser.put(userId, ranked);
            String mainTopic = recommendMapper.selectMainTopic(userId);
            if (mainTopic != null && !mainTopic.isEmpty()) {
                mainTopicByUser.put(userId, mainTopic);
            }
            int rank = ranked.indexOf(holdout) + 1;
            boolean hit = rank > 0;
            boolean hotHit = hotIds.contains(holdout);
            boolean latestHit = latestIds.contains(holdout);
            if (hit) {
                hits++;
                ndcgSum += 1d / (Math.log(rank + 1d) / Math.log(2d));
            }
            if (hotHit) {
                hotHits++;
            }
            if (latestHit) {
                latestHits++;
            }
            if (details.size() < MAX_DETAIL_ROWS) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("userId", userId);
                row.put("holdoutVideoId", holdout);
                row.put("rank", rank);
                row.put("hit", hit);
                row.put("hotHit", hotHit);
                row.put("latestHit", latestHit);
                row.put("top3", ranked.stream().limit(3).toList());
                details.add(row);
            }
        }

        // 兴趣对齐率：推荐结果命中用户主兴趣话题的比例。
        // HR 受曝光偏差影响（用户看的多是热门），这个指标直接看个性化有没有生效。
        List<Long> idForTopic = new ArrayList<>();
        rankedByUser.values().forEach(idForTopic::addAll);
        idForTopic.addAll(hotIds);
        Map<Long, String> topicOf = new HashMap<>();
        if (!idForTopic.isEmpty()) {
            List<Post> pairs = recommendMapper.selectTopicPairs(idForTopic.stream().distinct().toList());
            if (pairs != null) {
                for (Post post : pairs) {
                    topicOf.put(post.getId(), post.getTopic());
                }
            }
        }
        double recAlignSum = 0d;
        double hotAlignSum = 0d;
        int alignUsers = 0;
        for (Map.Entry<Long, String> entry : mainTopicByUser.entrySet()) {
            String topic = entry.getValue();
            List<Long> ranked = rankedByUser.getOrDefault(entry.getKey(), List.of());
            if (ranked.isEmpty()) {
                continue;
            }
            long matched = ranked.stream().filter(id -> topic.equals(topicOf.get(id))).count();
            long hotMatched = hotIds.stream().filter(id -> topic.equals(topicOf.get(id))).count();
            recAlignSum += (double) matched / ranked.size();
            hotAlignSum += (double) hotMatched / Math.max(1, hotIds.size());
            alignUsers++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("topK", k);
        result.put("candidateUsers", userIds.size());
        result.put("evaluatedUsers", evaluated);
        result.put("publicVideos", totalVideos);
        result.put("hr", ratio(hits, evaluated));
        result.put("ndcg", round(evaluated == 0 ? 0d : ndcgSum / evaluated));
        result.put("baselineHotHr", ratio(hotHits, evaluated));
        result.put("baselineLatestHr", ratio(latestHits, evaluated));
        result.put("baselineRandomHr", ratio(k, Math.max(1, totalVideos)));
        result.put("recInterestAlign", alignUsers == 0 ? 0d : round(recAlignSum / alignUsers));
        result.put("hotInterestAlign", alignUsers == 0 ? 0d : round(hotAlignSum / alignUsers));
        result.put("details", details);
        result.put("note", "leave-one-out，画像含 holdout 自身行为，指标用于横向对比");
        return result;
    }

    private static double ratio(int numerator, int denominator) {
        return round(denominator <= 0 ? 0d : (double) numerator / denominator);
    }

    private static double round(double value) {
        return Math.round(value * 10000d) / 10000d;
    }
}
