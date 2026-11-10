package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {
    public static final String CONTENT_TYPE_POST = "POST";
    public static final String CONTENT_TYPE_VIDEO = "VIDEO";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String title;
    private String content;
    private String cover;
    private String topic;
    private String contentType;
    private String videoUrl;
    private Integer likeCount;
    private Integer commentCount;
    private Integer collectCount;
    private Integer isPinned;
    private Integer isFeatured;
    private LocalDateTime pinnedTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
