package com.example.petcommunity3.service;

import com.example.petcommunity3.vo.*;

import java.util.List;

public interface StatisticsService {
    // 综合看板：一次性返回后台首页需要的多组统计数据。
    StatisticsDashboardVO dashboard();

    // 基础总览：用户数、宠物数、帖子数、订单数、订单总金额等。
    StatisticsOverviewVO overview();

    // 热门商品：按销量排序取前 10。
    List<TopProductVO> productTop10();

    // 热门话题：按帖子数量排序取前 10。
    List<TopTopicVO> topicTop10();

    // 预约状态统计：统计取消、已预约、已完成的数量。
    List<StatusCountVO> appointmentStatus();

    // 订单状态统计：统计取消、已下单、已完成的数量。
    List<StatusCountVO> orderStatus();

    // 增强统计：包含今日数据、趋势、转化率和热门帖子。
    StatisticsEnhancedVO enhanced(Integer days);

    // 每日趋势：按天统计用户、帖子、预约、订单等数据。
    List<DailyTrendVO> dailyTrend(Integer days);

    // 预约转化率：统计预约完成率和用户预约率。
    AppointmentConversionVO appointmentConversion();

    // 热门帖子：按点赞、评论、收藏综合得分排序。
    List<TopPostVO> hotPosts(Integer limit);
}