package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class CartAddDTO {
    private Long userId;
    private Long productId;
    private Integer quantity;
}