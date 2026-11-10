package com.example.petcommunity3.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyTrendVO {
    private LocalDate date;
    private Long userCount;
    private Long postCount;
    private Long appointmentCount;
    private Long orderCount;
    private BigDecimal orderAmount;
}
