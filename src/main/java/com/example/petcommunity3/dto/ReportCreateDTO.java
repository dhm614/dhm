package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class ReportCreateDTO {
    private String targetType;
    private Long targetId;
    private Long userId;
    private String reason;
}