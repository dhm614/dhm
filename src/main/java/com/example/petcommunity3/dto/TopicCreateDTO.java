package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class TopicCreateDTO {
    private String name;
    private String description;
    private String cover;
    private Integer sort;
}