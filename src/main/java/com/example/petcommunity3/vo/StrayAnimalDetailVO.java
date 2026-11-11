package com.example.petcommunity3.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StrayAnimalDetailVO {
    private Long id;
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
}
