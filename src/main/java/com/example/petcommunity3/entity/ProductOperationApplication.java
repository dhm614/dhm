package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product_operation_application")
public class ProductOperationApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantUserId;
    private Long productId;
    private String operationType;
    private String productName;
    private String category;
    private BigDecimal price;
    private Integer stock;
    private String cover;
    private String description;
    private Integer status;
    private Long reviewerId;
    private String reviewRemark;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
