package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.StrayAnimalReportCreateDTO;
import com.example.petcommunity3.dto.StrayAnimalReportReviewDTO;
import com.example.petcommunity3.entity.StrayAnimalReport;

import java.util.List;

public interface StrayAnimalReportService {
    void submit(Long userId, StrayAnimalReportCreateDTO dto);

    List<StrayAnimalReport> listMine(Long userId);

    List<StrayAnimalReport> listForAdmin(Integer status);

    StrayAnimalReport getForAdmin(Long id);

    void review(Long reportId, Long reviewerId, StrayAnimalReportReviewDTO dto);
}
