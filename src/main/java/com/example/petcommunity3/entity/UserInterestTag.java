package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_interest_tag")
public class UserInterestTag {
    public static final String TYPE_TOPIC = "TOPIC";
    public static final String TYPE_SPECIES = "SPECIES";
    public static final String TYPE_AUTHOR = "AUTHOR";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String tagType;
    private String tagValue;
    private BigDecimal weight;
    private LocalDateTime updateTime;
}
