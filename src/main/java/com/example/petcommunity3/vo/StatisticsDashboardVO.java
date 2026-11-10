package com.example.petcommunity3.vo;

import lombok.Data;

import java.util.List;

@Data
public class StatisticsDashboardVO {
    private StatisticsOverviewVO overview;
    private List<TopProductVO> productTop10;
    private List<TopTopicVO> topicTop10;
    private List<StatusCountVO> appointmentStatus;
    private List<StatusCountVO> orderStatus;
    private StatisticsEnhancedVO enhanced;
}
