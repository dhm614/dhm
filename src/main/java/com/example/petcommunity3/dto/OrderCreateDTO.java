package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class OrderCreateDTO {
    private Long userId;
    private String remark;
}