package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appointment")
public class Appointment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long petId;
    private Long storeId;
    private Long serviceItemId;
    private Long serviceScheduleId;
    private LocalDateTime appointmentTime;
    private String remark;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
