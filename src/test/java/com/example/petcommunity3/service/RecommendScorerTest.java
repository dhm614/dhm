package com.example.petcommunity3.service;

import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.VideoStat;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 打分公式的纯计算测试，不依赖数据库。
 */
class RecommendScorerTest {

    private final RecommendScorer scorer = new RecommendScorer();

    @Test
    void wilsonLowerBoundShouldBeZeroWhenNoPlay() {
        assertEquals(0d, scorer.wilsonLowerBound(0, 0, RecommendScorer.WILSON_Z), 1e-9);
    }

    @Test
    void wilsonLowerBoundShouldPenalizeSmallSamples() {
        double few = scorer.wilsonLowerBound(2, 2, RecommendScorer.WILSON_Z);
        double many = scorer.wilsonLowerBound(200, 200, RecommendScorer.WILSON_Z);
        assertTrue(few < many, "样本少的完播率置信下界应更低");
    }

    @Test
    void wilsonLowerBoundShouldGrowWithCompletionRate() {
        double low = scorer.wilsonLowerBound(3, 10, RecommendScorer.WILSON_Z);
        double high = scorer.wilsonLowerBound(9, 10, RecommendScorer.WILSON_Z);
        assertTrue(high > low);
    }

    @Test
    void freshnessShouldHalveAt72Hours() {
        Post post = new Post();
        post.setCreateTime(LocalDateTime.now().minusHours(72));
        assertEquals(0.5d, scorer.freshness(post, LocalDateTime.now()), 0.01d);
    }

    @Test
    void freshnessShouldBeOneForBrandNewPost() {
        Post post = new Post();
        post.setCreateTime(LocalDateTime.now());
        assertEquals(1d, scorer.freshness(post, LocalDateTime.now()), 0.01d);
    }

    @Test
    void interestMatchShouldWeightTopicAndAuthor() {
        Post post = new Post();
        post.setTopic("新手养猫");
        post.setUserId(7L);

        Map<String, Double> topicWeights = new HashMap<>();
        topicWeights.put("新手养猫", RecommendScorer.MAX_TAG_WEIGHT);
        Map<String, Double> authorWeights = new HashMap<>();
        authorWeights.put("7", RecommendScorer.MAX_TAG_WEIGHT);

        // 0.7 * 1.0 + 0.3 * 1.0
        assertEquals(1d, scorer.interestMatch(post, topicWeights, authorWeights), 1e-9);
        // 只有话题命中：0.7
        assertEquals(0.7d, scorer.interestMatch(post, topicWeights, new HashMap<>()), 1e-9);
        // 只有作者命中：0.3
        assertEquals(0.3d, scorer.interestMatch(post, new HashMap<>(), authorWeights), 1e-9);
    }

    @Test
    void interestMatchShouldClampToUnitRange() {
        Post post = new Post();
        post.setTopic("狗狗训练");
        Map<String, Double> topicWeights = new HashMap<>();
        // 超过归一化上限的异常值应被裁剪为 1
        topicWeights.put("狗狗训练", 999d);
        assertEquals(0.7d, scorer.interestMatch(post, topicWeights, new HashMap<>()), 1e-9);
    }

    @Test
    void weightedEngagementShouldSumWithWeights() {
        VideoStat stat = VideoStat.empty(1L);
        stat.setLikeCnt(1);
        stat.setCommentCnt(2);
        stat.setCollectCnt(2);
        stat.setShareCnt(3);
        // 1*1 + 2*2 + 2*2 + 3*3
        assertEquals(18d, scorer.weightedEngagement(stat), 1e-9);
        assertEquals(0d, scorer.weightedEngagement(null), 1e-9);
    }

    @Test
    void computeScoreShouldIncreaseWithInterest() {
        RecommendConfigService config = mock(RecommendConfigService.class);
        when(config.getDouble(anyString(), anyDouble())).thenAnswer(inv -> inv.getArgument(1));
        when(config.getInt(anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(inv -> inv.getArgument(1));
        RecommendWeights weights = RecommendWeights.load(config);

        Post post = new Post();
        post.setCreateTime(LocalDateTime.now());
        VideoStat stat = VideoStat.empty(1L);
        stat.setPlayCnt(10);
        stat.setPlayEndCnt(10);
        stat.setAvgWatchRatio(new BigDecimal("1.000"));

        double cold = scorer.computeScore(stat, post, 0d, 0d, 18d, LocalDateTime.now(), weights);
        double warm = scorer.computeScore(stat, post, 1d, 0d, 18d, LocalDateTime.now(), weights);
        assertTrue(warm > cold, "兴趣匹配应抬高分数");
        assertTrue(Math.abs(weights.sum() - 1d) < 1e-9, "默认权重之和应为 1");
    }
}
