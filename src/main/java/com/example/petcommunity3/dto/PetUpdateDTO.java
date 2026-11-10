package com.example.petcommunity3.dto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PetUpdateDTO {
    private Long id;
    private Long userId;
    private String name;
    private String species;
    private String breed;
    private Integer age;
    private BigDecimal weight;
    private String gender;
    private Integer vaccinated;
    private Integer neutered;
    private String avatar;
    private String remark;
}
