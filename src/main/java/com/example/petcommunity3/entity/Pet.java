package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pet")
public class Pet {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String name;
    private String species;
    private String breed;
    private Integer age;
    private BigDecimal weight;
    private String gender;
    private Integer vaccinated;
    private Integer neutered;
    private String avatar;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
