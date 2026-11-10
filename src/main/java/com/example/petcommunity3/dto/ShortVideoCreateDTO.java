package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class ShortVideoCreateDTO {
    private Long userId;
    private String title;
    private String content;
    private String cover;
    private String videoUrl;
    private String topic;
}
