package com.example.petcommunity3.job;

import com.example.petcommunity3.service.RecommendAggregateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 推荐离线聚合定时任务：启动 1 分钟后首次执行，之后每 5 分钟重算一次热度/兴趣/相似度。
 * 单机小数据量场景足够；数据规模上来后应换成独立批处理。
 */
@Component
public class RecommendAggregateJob {
    private static final Logger log = LoggerFactory.getLogger(RecommendAggregateJob.class);

    private final RecommendAggregateService aggregateService;

    public RecommendAggregateJob(RecommendAggregateService aggregateService) {
        this.aggregateService = aggregateService;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 300_000)
    public void refresh() {
        try {
            var result = aggregateService.refreshAll();
            log.info("recommend aggregate finished: stat={}, tag={}, similar={}, cost={}ms",
                    result.getStatRows(), result.getTagRows(), result.getSimilarRows(), result.getCostMs());
        } catch (Exception e) {
            log.error("recommend aggregate failed", e);
        }
    }
}
