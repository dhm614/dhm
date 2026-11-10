package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("merchant_application")
public class MerchantApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String merchantName;
    private String contactName;
    private String contactPhone;
    private String businessType;
    private String businessAddress;
    private String reason;
    private String attachmentName;
    private String attachmentUrl;
    private Integer status;
    private Long reviewerId;
    private String reviewRemark;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
