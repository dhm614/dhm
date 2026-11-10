package com.example.petcommunity3.vo;

import com.example.petcommunity3.entity.Store;
import lombok.Data;

@Data
public class StoreDistanceVO {
    private Store store;
    private Double distanceKm;
}
