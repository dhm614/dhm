package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.StrayAnimalUpdateDTO;
import com.example.petcommunity3.entity.StrayAnimal;
import com.example.petcommunity3.vo.StrayAnimalDetailVO;
import com.example.petcommunity3.vo.StrayAnimalDistanceVO;

import java.math.BigDecimal;
import java.util.List;

public interface StrayAnimalService {
    List<StrayAnimalDistanceVO> listNearby(BigDecimal longitude,
                                           BigDecimal latitude,
                                           Integer limit,
                                           Long viewerId);

    StrayAnimalDetailVO getDetail(Long id, Long viewerId);

    List<StrayAnimal> listForAdmin(Integer rescueStatus);

    void updateForAdmin(Long id, StrayAnimalUpdateDTO dto);
}
