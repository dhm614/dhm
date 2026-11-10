package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.service.StatisticsService;
import com.example.petcommunity3.vo.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    // 数据统计接口入口：给后台看板提供总览、排行、趋势和转化率数据。
    private final StatisticsService statisticsService;

    // 注入统计业务层，Controller 不直接统计数据库。
    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    // GET /api/statistics/dashboard
    // 返回后台首页需要的综合看板数据。
    @GetMapping("/dashboard")
    public Result<StatisticsDashboardVO> dashboard() {
        return Result.success(statisticsService.dashboard());
    }

    // GET /api/statistics/overview
    // 返回用户数、宠物数、帖子数、订单总金额等核心指标。
    @GetMapping("/overview")
    public Result<StatisticsOverviewVO> overview() {
        return Result.success(statisticsService.overview());
    }

    // GET /api/statistics/product-top10
    // 按销量查询热门商品前 10 名。
    @GetMapping("/product-top10")
    public Result<List<TopProductVO>> productTop10() {
        return Result.success(statisticsService.productTop10());
    }

    // GET /api/statistics/topic-top10
    // 按帖子数查询热门话题前 10 名。
    @GetMapping("/topic-top10")
    public Result<List<TopTopicVO>> topicTop10() {
        return Result.success(statisticsService.topicTop10());
    }

    // GET /api/statistics/appointment-status
    // 统计预约在取消、已预约、已完成这几种状态下的数量。
    @GetMapping("/appointment-status")
    public Result<List<StatusCountVO>> appointmentStatus() {
        return Result.success(statisticsService.appointmentStatus());
    }

    // GET /api/statistics/order-status
    // 统计订单在取消、已下单、已完成这几种状态下的数量。
    @GetMapping("/order-status")
    public Result<List<StatusCountVO>> orderStatus() {
        return Result.success(statisticsService.orderStatus());
    }

    // GET /api/statistics/enhanced?days=7
    // 返回增强统计，包括今日数据、趋势数据、转化率和热门帖子。
    @GetMapping("/enhanced")
    public Result<StatisticsEnhancedVO> enhanced(@RequestParam(required = false) Integer days) {
        return Result.success(statisticsService.enhanced(days));
    }

    // GET /api/statistics/daily-trend?days=7
    // 查询最近 days 天的每日趋势数据，days 不传时由 Service 给默认值。
    @GetMapping("/daily-trend")
    public Result<List<DailyTrendVO>> dailyTrend(@RequestParam(required = false) Integer days) {
        return Result.success(statisticsService.dailyTrend(days));
    }

    // GET /api/statistics/appointment-conversion
    // 查询预约完成率和用户预约转化率。
    @GetMapping("/appointment-conversion")
    public Result<AppointmentConversionVO> appointmentConversion() {
        return Result.success(statisticsService.appointmentConversion());
    }

    // GET /api/statistics/hot-posts?limit=10
    // 根据点赞、评论、收藏综合分数查询热门帖子。
    @GetMapping("/hot-posts")
    public Result<List<TopPostVO>> hotPosts(@RequestParam(required = false) Integer limit) {
        return Result.success(statisticsService.hotPosts(limit));
    }
}
