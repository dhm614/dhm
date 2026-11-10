package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("store")
public class Store {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantUserId;
    private String name;
    private String address;
    private String phone;
    private String cover;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String businessHours;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
