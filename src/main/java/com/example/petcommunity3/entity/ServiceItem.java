package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("service_item")
public class ServiceItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long storeId;
    private String name;
    private String category;
    private BigDecimal price;
    private Integer duration;
    private String cover;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}