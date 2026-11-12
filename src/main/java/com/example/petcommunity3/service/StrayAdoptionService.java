package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.StrayAdoptionCreateDTO;
import com.example.petcommunity3.dto.StrayAdoptionReviewDTO;
import com.example.petcommunity3.entity.StrayAdoptionApplication;

import java.util.List;

public interface StrayAdoptionService {
    void submit(Long userId, StrayAdoptionCreateDTO dto);

    List<StrayAdoptionApplication> listMine(Long userId);

    void cancel(Long applicationId, Long userId);

    List<StrayAdoptionApplication> listForAdmin(Integer status);

    void review(Long applicationId, Long reviewerId, StrayAdoptionReviewDTO dto);
}
