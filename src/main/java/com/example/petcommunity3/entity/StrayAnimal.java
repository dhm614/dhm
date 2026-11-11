package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stray_animal")
public class StrayAnimal {
    public static final int RESCUE_WAITING = 0;
    public static final int RESCUE_ADOPTION_PENDING = 1;
    public static final int RESCUE_ADOPTED = 2;
    public static final int RESCUE_CLOSED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
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
    private String healthStatus;
    private Integer dangerLevel;
    private Integer rescueStatus;
    private String professionalName;
    private String professionalOrganization;
    private String professionalPhone;
    private String captureInstruction;
    private String dewormingAdvice;
    private String vaccineAdvice;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
