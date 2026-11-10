package com.example.petcommunity3.vo;

import lombok.Data;

@Data
public class TopPostVO {
    private Long postId;
    private String title;
    private String topic;
    private Integer likeCount;
    private Integer commentCount;
    private Integer collectCount;
    private Integer score;
}
