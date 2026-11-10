package com.example.petcommunity3.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopProductVO {
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer sales;
    private Integer stock;
}