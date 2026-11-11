package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class StrayAnimalReportReviewDTO {
    private Integer status;
    private String reviewRemark;
    private String professionalName;
    private String professionalOrganization;
    private String professionalPhone;
    private String captureInstruction;
    private String dewormingAdvice;
    private String vaccineAdvice;
}
