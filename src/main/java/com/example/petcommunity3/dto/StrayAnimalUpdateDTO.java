package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class StrayAnimalUpdateDTO {
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
}
