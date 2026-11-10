package com.example.petcommunity3.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCreateDTO {
    private String name;
    private String category;
    private BigDecimal price;
    private Integer stock;
    private String cover;
    private String description;
}