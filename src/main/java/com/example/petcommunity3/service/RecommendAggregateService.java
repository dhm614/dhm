package com.example.petcommunity3.service;

import com.example.petcommunity3.vo.AggregateResultVO;

public interface RecommendAggregateService {
    /**
     * 全量重算：视频热度、用户兴趣标签、相似视频。
     */
    AggregateResultVO refreshAll();
}
