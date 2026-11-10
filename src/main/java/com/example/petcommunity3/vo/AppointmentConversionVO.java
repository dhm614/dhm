package com.example.petcommunity3.vo;

import lombok.Data;

@Data
public class AppointmentConversionVO {
    private Long totalAppointmentCount;
    private Long finishedAppointmentCount;
    private Double finishRate;
    private Long userCount;
    private Double userAppointmentRate;
}
