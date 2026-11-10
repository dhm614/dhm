package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class FollowActionDTO {
    private Long followerId;
    private Long followedId;
}