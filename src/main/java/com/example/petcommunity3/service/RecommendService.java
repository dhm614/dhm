package com.example.petcommunity3.service;

import com.example.petcommunity3.vo.VideoFeedVO;

public interface RecommendService {
    /**
     * 短视频推荐流。userId 可为空（未登录），此时退化为热门+新品。
     *
     * @param offset 分页偏移，对应已消费的条数
     */
    VideoFeedVO recommend(Long userId, int limit, int offset, String traceId);

    /**
     * 用户行为变化后清理其推荐缓存。
     */
    void invalidateCache(Long userId);
}
