package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantUserId;
    private String name;
    private String category;
    private BigDecimal price;
    private Integer stock;
    private String cover;
    private String description;
    private Integer sales;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
