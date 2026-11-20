package com.example.petcommunity3.service;

import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.VideoStat;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 推荐打分的纯计算部分，与存储解耦，方便单测和离线评估复用。
 *
 * 公式（权重可由 recommend_config 动态调整）：
 *   score = w.quality * Wilson(完播/播放)
 *         + w.completeness * 平均观看比例
 *         + w.engagement * 互动强度对数归一化
 *         + w.freshness * 72 小时半衰
 *         + w.interest * 长期兴趣匹配（话题 + 作者）
 *         + w.similarity * 短期兴趣相似度（ItemCF）
 */
@Component
public class RecommendScorer {
    /** Wilson 置信区间 z 值，95% 置信水平。 */
    public static final double WILSON_Z = 1.96d;
    /** 新鲜度半衰期（小时）。 */
    public static final double HALF_LIFE_HOURS = 72d;
    /** 兴趣标签权重归一化的上限（聚合时标签最大权重被归一到该值）。 */
    public static final double MAX_TAG_WEIGHT = 10d;

    public double interestMatch(Post post, Map<String, Double> topicWeights, Map<String, Double> authorWeights) {
        // 话题与作者各自先归一化裁剪，避免某一项权重异常时把另一项的分也吃掉
        double topic = 0d;
        if (post.getTopic() != null && !post.getTopic().isEmpty()) {
            topic = clamp01(valueOf(topicWeights, post.getTopic()) / MAX_TAG_WEIGHT);
        }
        double author = post.getUserId() == null ? 0d
                : clamp01(valueOf(authorWeights, String.valueOf(post.getUserId())) / MAX_TAG_WEIGHT);
        return clamp01(0.7d * topic + 0.3d * author);
    }

    public double weightedEngagement(VideoStat stat) {
        if (stat == null) {
            return 0d;
        }
        return safe(stat.getLikeCnt()) * 1.0d
                + safe(stat.getCommentCnt()) * 2.0d
                + safe(stat.getCollectCnt()) * 2.0d
                + safe(stat.getShareCnt()) * 3.0d;
    }

    public double wilsonLowerBound(int positives, int total, double z) {
        if (total <= 0) {
            return 0d;
        }
        double p = (double) positives / total;
        double denominator = 1d + z * z / total;
        double center = p + z * z / (2d * total);
        double margin = z * Math.sqrt(p * (1d - p) / total + z * z / (4d * total * total));
        return Math.max(0d, (center - margin) / denominator);
    }

    public double freshness(Post post, LocalDateTime now) {
        if (post.getCreateTime() == null || now == null) {
            return 0d;
        }
        double hours = Math.max(0d, Duration.between(post.getCreateTime(), now).toMinutes() / 60d);
        return Math.pow(0.5d, hours / HALF_LIFE_HOURS);
    }

    public double computeScore(VideoStat stat, Post post, double interest, double similarity,
                               double maxEngagement, LocalDateTime now, RecommendWeights weights) {
        int plays = stat == null ? 0 : safe(stat.getPlayCnt());
        int finishes = stat == null ? 0 : safe(stat.getPlayEndCnt());
        double quality = wilsonLowerBound(finishes, plays, WILSON_Z);

        double completeness = stat == null || stat.getAvgWatchRatio() == null
                ? 0d : clamp01(stat.getAvgWatchRatio().doubleValue());

        double engagement = maxEngagement <= 0d
                ? 0d : Math.log1p(weightedEngagement(stat)) / Math.log1p(maxEngagement);

        double freshness = freshness(post, now);

        return weights.quality * quality
                + weights.completeness * completeness
                + weights.engagement * engagement
                + weights.freshness * freshness
                + weights.interest * clamp01(interest)
                + weights.similarity * clamp01(similarity);
    }

    public static double clamp01(double value) {
        if (Double.isNaN(value)) {
            return 0d;
        }
        return Math.min(1d, Math.max(0d, value));
    }

    private static double valueOf(Map<String, Double> map, String key) {
        if (map == null || key == null) {
            return 0d;
        }
        Double value = map.get(key);
        return value == null ? 0d : value;
    }

    private static int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
