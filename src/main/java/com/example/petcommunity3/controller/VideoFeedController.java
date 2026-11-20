package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.service.RecommendService;
import com.example.petcommunity3.vo.VideoFeedVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 推荐流入口。单独挂在 /api/feed 下，避免与 /api/videos/{id} 的路径变量冲突。
 */
@RestController
@RequestMapping("/api/feed")
public class VideoFeedController {
    private final RecommendService recommendService;

    public VideoFeedController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping("/videos")
    public Result<VideoFeedVO> videos(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String traceId) {
        return Result.success(recommendService.recommend(userId, limit, offset, traceId));
    }
}
