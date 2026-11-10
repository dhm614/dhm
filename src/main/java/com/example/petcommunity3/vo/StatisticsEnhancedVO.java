package com.example.petcommunity3.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StatisticsEnhancedVO {
    private Long todayActiveUserCount;
    private Long todayPostCount;
    private Long todayAppointmentCount;
    private Long todayOrderCount;
    private BigDecimal todayOrderAmount;
    private AppointmentConversionVO appointmentConversion;
    private List<DailyTrendVO> dailyTrend;
    private List<TopPostVO> hotPosts;
}
