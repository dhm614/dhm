package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.BehaviorEventDTO;
import com.example.petcommunity3.entity.UserBehavior;
import com.example.petcommunity3.service.BehaviorService;
import com.example.petcommunity3.service.RecommendService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/behaviors")
public class BehaviorController {
    private static final Set<String> CACHE_BREAKING_ACTIONS = Set.of(
            UserBehavior.ACTION_LIKE,
            UserBehavior.ACTION_COMMENT,
            UserBehavior.ACTION_COLLECT,
            UserBehavior.ACTION_SHARE,
            UserBehavior.ACTION_PLAY_END
    );

    private final BehaviorService behaviorService;
    private final RecommendService recommendService;

    public BehaviorController(BehaviorService behaviorService, RecommendService recommendService) {
        this.behaviorService = behaviorService;
        this.recommendService = recommendService;
    }

    @PostMapping
    public Result<Void> record(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody BehaviorEventDTO dto) {
        behaviorService.record(userId, dto);
        if (dto != null && CACHE_BREAKING_ACTIONS.contains(dto.getAction())) {
            recommendService.invalidateCache(userId);
        }
        return Result.success(null);
    }
}
