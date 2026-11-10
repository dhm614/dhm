package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class SensitiveWordCreateDTO {
    private String word;
    private Integer level;
}