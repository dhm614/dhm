package com.example.petcommunity3.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StatisticsOverviewVO {
    private Long userCount;
    private Long petCount;
    private Long postCount;
    private Long commentCount;
    private Long appointmentCount;
    private Long productCount;
    private Long orderCount;
    private BigDecimal totalOrderAmount;
}