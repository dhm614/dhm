package com.example.petcommunity3.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StoreCreateDTO {
    private String name;
    private String address;
    private String phone;
    private String cover;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String businessHours;
    private String description;
}