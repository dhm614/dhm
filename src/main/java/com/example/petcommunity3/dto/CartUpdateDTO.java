package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class CartUpdateDTO {
    private Long id;
    private Long userId;
    private Integer quantity;
}