package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("service_schedule")
public class ServiceSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long storeId;
    private Long serviceItemId;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private Integer bookedCount;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
