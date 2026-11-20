package com.example.petcommunity3.service;

/**
 * 一次推荐请求使用的权重与限额快照。
 * 从 {@link RecommendConfigService} 读出，避免打分循环里反复查配置。
 */
public final class RecommendWeights {

    public final double quality;
    public final double completeness;
    public final double engagement;
    public final double freshness;
    public final double interest;
    public final double similarity;
    public final int maxPerAuthor;
    public final int maxPerTopic;
    public final int maxCandidates;
    public final double seenPenalty;
    public final long feedCacheTtlMs;

    private RecommendWeights(double quality, double completeness, double engagement,
                             double freshness, double interest, double similarity,
                             int maxPerAuthor, int maxPerTopic, int maxCandidates,
                             double seenPenalty, long feedCacheTtlMs) {
        this.quality = quality;
        this.completeness = completeness;
        this.engagement = engagement;
        this.freshness = freshness;
        this.interest = interest;
        this.similarity = similarity;
        this.maxPerAuthor = maxPerAuthor;
        this.maxPerTopic = maxPerTopic;
        this.maxCandidates = maxCandidates;
        this.seenPenalty = seenPenalty;
        this.feedCacheTtlMs = feedCacheTtlMs;
    }

    public static RecommendWeights load(RecommendConfigService config) {
        return new RecommendWeights(
                config.getDouble("weight.quality", 0.18d),
                config.getDouble("weight.completeness", 0.05d),
                config.getDouble("weight.engagement", 0.05d),
                config.getDouble("weight.freshness", 0.07d),
                config.getDouble("weight.interest", 0.53d),
                config.getDouble("weight.similarity", 0.12d),
                config.getInt("diversity.max_per_author", 2),
                config.getInt("diversity.max_per_topic", 3),
                config.getInt("recall.max_candidates", 60),
                config.getDouble("seen.penalty", 0.65d),
                Math.max(1_000L, config.getInt("cache.feed_ttl_ms", 120_000)));
    }

    public double sum() {
        return quality + completeness + engagement + freshness + interest + similarity;
    }
}
