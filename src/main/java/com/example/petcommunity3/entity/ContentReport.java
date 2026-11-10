package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_report")
public class ContentReport {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String targetType;
    private Long targetId;
    private Long userId;
    private String reason;
    private Integer status;
    private String handleResult;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}