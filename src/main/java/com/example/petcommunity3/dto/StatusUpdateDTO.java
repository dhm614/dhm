package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class StatusUpdateDTO {
    private Integer status;
    private Long operatorId;
    private String remark;
}
