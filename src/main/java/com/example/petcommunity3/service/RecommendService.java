package com.example.petcommunity3.service;

import com.example.petcommunity3.vo.VideoFeedVO;

import java.util.Collection;
import java.util.List;

public interface RecommendService {
    /**
     * 短视频推荐流。userId 可为空（未登录），此时退化为热门+新品。
     *
     * @param offset 分页偏移，对应已消费的条数
     */
    VideoFeedVO recommend(Long userId, int limit, int offset, String traceId);

    /**
     * 只排序不埋点，供离线评估使用：跳过缓存、跳过曝光写入。
     *
     * @param excludeIds 需要从候选中剔除的视频（如 leave-one-out 的 holdout）
     */
    List<Long> rankVideoIds(Long userId, Collection<Long> excludeIds, int limit);

    /**
     * 离线评估专用排序：不套「已看过降权」。
     *
     * holdout 是用户真实交互过的视频，线上会被 seen 惩罚压后；
     * 评估关心的是「不惩罚的话它能不能排进前列」，所以这里关掉该惩罚。
     */
    List<Long> rankVideoIdsForEvaluation(Long userId, int limit);

    /**
     * 用户行为变化后清理其推荐缓存。
     */
    void invalidateCache(Long userId);
}
