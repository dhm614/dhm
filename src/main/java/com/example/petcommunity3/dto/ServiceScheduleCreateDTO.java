package com.example.petcommunity3.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ServiceScheduleCreateDTO {
    private Long storeId;
    private Long serviceItemId;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private String remark;
}
