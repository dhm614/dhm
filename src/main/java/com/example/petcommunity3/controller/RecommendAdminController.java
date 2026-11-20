package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.service.AdminService;
import com.example.petcommunity3.service.RecommendAggregateService;
import com.example.petcommunity3.service.impl.MockDataService;
import com.example.petcommunity3.vo.AggregateResultVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 推荐模块运维接口：手动触发聚合、生成模拟数据。
 */
@RestController
@RequestMapping("/api/admin/recommend")
public class RecommendAdminController {
    private final AdminService adminService;
    private final RecommendAggregateService aggregateService;
    private final MockDataService mockDataService;

    public RecommendAdminController(AdminService adminService,
                                    RecommendAggregateService aggregateService,
                                    MockDataService mockDataService) {
        this.adminService = adminService;
        this.aggregateService = aggregateService;
        this.mockDataService = mockDataService;
    }

    @PostMapping("/aggregate")
    public Result<AggregateResultVO> aggregate(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId) {
        adminService.requireAdmin(operatorId);
        return Result.success(aggregateService.refreshAll());
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
