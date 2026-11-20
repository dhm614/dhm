package com.example.petcommunity3.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoFeedItemVO {
    private Long id;
    private String title;
    private String content;
    private String cover;
    private String videoUrl;
    private String topic;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private Integer likeCount;
    private Integer commentCount;
    private Integer collectCount;
    private LocalDateTime createTime;
    private Double hotScore;
    /** 召回来源：HOT/FOLLOW/TOPIC/AUTHOR/SIMILAR/FRESH/LATEST */
    private String reason;
}
