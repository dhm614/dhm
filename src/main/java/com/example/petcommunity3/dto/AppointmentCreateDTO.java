package com.example.petcommunity3.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentCreateDTO {
    private Long userId;
    private Long petId;
    private Long storeId;
    private Long serviceItemId;
    private Long serviceScheduleId;
    private LocalDateTime appointmentTime;
    private String remark;
}
