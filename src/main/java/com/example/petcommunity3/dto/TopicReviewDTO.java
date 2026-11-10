package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class TopicReviewDTO {
    private Integer status;
    private Integer sort;
    private Long operatorId;
    private String remark;
}
