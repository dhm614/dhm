package com.example.petcommunity3.service;

import com.example.petcommunity3.mapper.RecommendMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * 离线评估指标：命中率与单相关项 NDCG。
 */
@ExtendWith(MockitoExtension.class)
class RecommendEvaluationServiceTest {

    @Mock
    private RecommendMapper recommendMapper;
    @Mock
    private RecommendService recommendService;

    @Test
    void shouldComputeHitRateAndNdcg() {
        when(recommendMapper.selectEvaluableUserIds(anyInt(), anyInt())).thenReturn(List.of(1L, 2L));
        when(recommendMapper.selectLatestPositiveTarget(1L)).thenReturn(100L);
        when(recommendMapper.selectLatestPositiveTarget(2L)).thenReturn(200L);
        when(recommendMapper.selectHot(anyInt())).thenReturn(List.of());
        when(recommendMapper.selectLatest(anyInt())).thenReturn(List.of());
        when(recommendMapper.countPublicVideos()).thenReturn(1000);

        // 用户 1 的 holdout 排第 1，用户 2 未命中
        when(recommendService.rankVideoIdsForEvaluation(1L, 10))
                .thenReturn(List.of(100L, 5L, 6L));
        when(recommendService.rankVideoIdsForEvaluation(2L, 10))
                .thenReturn(List.of(5L, 6L, 7L));

        RecommendEvaluationService service = new RecommendEvaluationService(recommendMapper, recommendService);
        Map<String, Object> result = service.evaluate(10, 10);

        assertEquals(2, result.get("evaluatedUsers"));
        assertEquals(0.5d, (double) result.get("hr"), 1e-9);
        assertEquals(0.5d, (double) result.get("ndcg"), 1e-9);
        assertEquals(0.01d, (double) result.get("baselineRandomHr"), 1e-9);
    }

    @Test
    void shouldReportBaselineHits() {
        when(recommendMapper.selectEvaluableUserIds(anyInt(), anyInt())).thenReturn(List.of(1L));
        when(recommendMapper.selectLatestPositiveTarget(1L)).thenReturn(100L);
        when(recommendMapper.selectHot(anyInt())).thenReturn(List.of(post(100L), post(2L)));
        when(recommendMapper.selectLatest(anyInt())).thenReturn(List.of(post(3L)));
        when(recommendMapper.countPublicVideos()).thenReturn(10);
        when(recommendService.rankVideoIdsForEvaluation(1L, 5))
                .thenReturn(List.of(1L, 2L, 3L, 4L, 5L));

        RecommendEvaluationService service = new RecommendEvaluationService(recommendMapper, recommendService);
        Map<String, Object> result = service.evaluate(5, 10);

        assertEquals(1d, (double) result.get("baselineHotHr"), 1e-9);
        assertEquals(0d, (double) result.get("baselineLatestHr"), 1e-9);
        assertEquals(0d, (double) result.get("hr"), 1e-9);
    }

    private static com.example.petcommunity3.entity.Post post(Long id) {
        com.example.petcommunity3.entity.Post post = new com.example.petcommunity3.entity.Post();
        post.setId(id);
        return post;
    }
}
