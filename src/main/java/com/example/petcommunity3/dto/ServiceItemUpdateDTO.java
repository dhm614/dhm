package com.example.petcommunity3.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceItemUpdateDTO {
    private Long id;
    private Long storeId;
    private String name;
    private String category;
    private BigDecimal price;
    private Integer duration;
    private String cover;
    private String description;
    private Integer status;
}