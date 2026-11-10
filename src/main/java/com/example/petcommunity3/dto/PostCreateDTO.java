package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class PostCreateDTO {
    private Long userId;
    private String title;
    private String content;
    private String cover;
    private String topic;
}