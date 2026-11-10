package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.ApplicationReviewDTO;
import com.example.petcommunity3.dto.MerchantApplicationCreateDTO;
import com.example.petcommunity3.entity.MerchantApplication;

import java.util.List;

public interface MerchantApplicationService {
    void submit(Long userId, MerchantApplicationCreateDTO dto);

    MerchantApplication getMyLatest(Long userId);

    List<MerchantApplication> list(Integer status);

    void review(Long applicationId, Long reviewerId, ApplicationReviewDTO dto);
}
