package com.example.petcommunity3.dto;

import lombok.Data;

@Data
public class StrayAdoptionCreateDTO {
    private Long animalId;
    private String contactPhone;
    private String livingCondition;
    private String petExperience;
    private String applicationReason;
}
