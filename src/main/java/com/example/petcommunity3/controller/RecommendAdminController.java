package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.service.AdminService;
import com.example.petcommunity3.service.RecommendAggregateService;
import com.example.petcommunity3.service.RecommendConfigService;
import com.example.petcommunity3.service.RecommendEvaluationService;
import com.example.petcommunity3.service.impl.MockDataService;
import com.example.petcommunity3.vo.AggregateResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 推荐模块运维接口：手动触发聚合、生成模拟数据、调参。
 */
@RestController
@RequestMapping("/api/admin/recommend")
public class RecommendAdminController {
    private final AdminService adminService;
    private final RecommendAggregateService aggregateService;
    private final RecommendConfigService configService;
    private final RecommendEvaluationService evaluationService;
    private final MockDataService mockDataService;

    public RecommendAdminController(AdminService adminService,
                                    RecommendAggregateService aggregateService,
                                    RecommendConfigService configService,
                                    RecommendEvaluationService evaluationService,
                                    MockDataService mockDataService) {
        this.adminService = adminService;
        this.aggregateService = aggregateService;
        this.configService = configService;
        this.evaluationService = evaluationService;
        this.mockDataService = mockDataService;
    }

    @PostMapping("/aggregate")
    public Result<AggregateResultVO> aggregate(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId) {
        adminService.requireAdmin(operatorId);
        return Result.success(aggregateService.refreshAll());
    }

    @GetMapping("/config")
    public Result<Map<String, String>> config(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId) {
        adminService.requireAdmin(operatorId);
        return Result.success(configService.all());
    }

    /**
     * 只更新传入的键，未传的保持原值；改完立刻生效（服务内部 60 秒缓存被清空）。
     */
    @PutMapping("/config")
    public Result<Map<String, String>> updateConfig(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId,
            @RequestBody Map<String, String> patch) {
        adminService.requireAdmin(operatorId);
        return Result.success(configService.update(patch));
    }

    @GetMapping("/evaluate")
    public Result<Map<String, Object>> evaluate(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId,
            @RequestParam(defaultValue = "10") int topK,
            @RequestParam(defaultValue = "50") int maxUsers) {
        adminService.requireAdmin(operatorId);
        return Result.success(evaluationService.evaluate(topK, maxUsers));
    }

    @PostMapping("/mock")
    public Result<Map<String, Object>> mock(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId,
            @RequestParam(defaultValue = "20") int users,
            @RequestParam(defaultValue = "120") int videos,
            @RequestParam(defaultValue = "8000") int behaviors) {
        adminService.requireAdmin(operatorId);
        return Result.success(mockDataService.generate(users, videos, behaviors));
    }
}
