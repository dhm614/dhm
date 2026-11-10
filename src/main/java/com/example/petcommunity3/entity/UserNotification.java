package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_notification")
public class UserNotification {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long fromUserId;
    private String type;
    private String content;
    private String targetType;
    private Long targetId;
    private Integer readStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}