package com.example.petcommunity3.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StrayAnimalReportCreateDTO {
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
    private String healthDescription;
    private Integer emergencyLevel;
    private String reporterPhone;
}
