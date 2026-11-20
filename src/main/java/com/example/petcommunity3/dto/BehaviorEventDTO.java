package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class BehaviorEventDTO {
    private Long targetId;
    private String targetType;
    private String action;
    private Integer watchMs;
    private Double watchRatio;
    private String scene;
    private String traceId;
}
