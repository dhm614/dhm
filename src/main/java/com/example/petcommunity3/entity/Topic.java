package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("topic")
public class Topic {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String cover;
    private Integer postCount;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}