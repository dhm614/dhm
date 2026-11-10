package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class PostCommentCreateDTO {
    private Long postId;
    private Long userId;
    private Long parentId;
    private String content;
}