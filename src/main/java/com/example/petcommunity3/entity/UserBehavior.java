package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_behavior")
public class UserBehavior {
    public static final String ACTION_IMPRESSION = "IMPRESSION";
    public static final String ACTION_PLAY = "PLAY";
    public static final String ACTION_PLAY_END = "PLAY_END";
    public static final String ACTION_LIKE = "LIKE";
    public static final String ACTION_COMMENT = "COMMENT";
    public static final String ACTION_COLLECT = "COLLECT";
    public static final String ACTION_SHARE = "SHARE";
    public static final String ACTION_SKIP = "SKIP";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String targetType;
    private Long targetId;
    private String action;
    private Integer watchMs;
    private BigDecimal watchRatio;
    private String scene;
    private String traceId;
    private LocalDateTime createTime;
}
