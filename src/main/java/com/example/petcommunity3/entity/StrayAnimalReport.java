package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stray_animal_report")
public class StrayAnimalReport {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reporterUserId;
    private String temporaryName;
    private String species;
    private String gender;
    private Integer estimatedAge;
    private String photoUrl;
    private String foundAddress;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private LocalDateTime foundTime;
    private String appearanceDescription;
    private String healthDescription;
    private Integer emergencyLevel;
    private String reporterPhone;
    private Integer status;
    private Long reviewerId;
    private String reviewRemark;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
