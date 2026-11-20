package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("video_similar")
public class VideoSimilar {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long videoId;
    private Long similarVideoId;
    private BigDecimal score;
    private LocalDateTime updateTime;
}
