package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class SensitiveWordUpdateDTO {
    private Long id;
    private String word;
    private Integer level;
    private Integer status;
}