package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class PostUpdateDTO {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String cover;
    private String topic;
}