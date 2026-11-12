package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.StrayAdoptionCreateDTO;
import com.example.petcommunity3.dto.StrayAnimalReportCreateDTO;
import com.example.petcommunity3.entity.StrayAdoptionApplication;
import com.example.petcommunity3.entity.StrayAnimalReport;
import com.example.petcommunity3.service.StrayAdoptionService;
import com.example.petcommunity3.service.StrayAnimalReportService;
import com.example.petcommunity3.service.StrayAnimalService;
import com.example.petcommunity3.vo.StrayAnimalDetailVO;
import com.example.petcommunity3.vo.StrayAnimalDistanceVO;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class StrayAnimalController {
    private final StrayAnimalReportService reportService;
    private final StrayAnimalService animalService;
    private final StrayAdoptionService adoptionService;

    public StrayAnimalController(StrayAnimalReportService reportService,
                                 StrayAnimalService animalService,
                                 StrayAdoptionService adoptionService) {
        this.reportService = reportService;
        this.animalService = animalService;
        this.adoptionService = adoptionService;
    }

    @PostMapping("/stray-animal-reports")
    public Result<Void> submitReport(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                     @RequestBody StrayAnimalReportCreateDTO dto) {
        reportService.submit(userId, dto);
        return Result.success(null);
    }

    @GetMapping("/stray-animal-reports/my")
    public Result<List<StrayAnimalReport>> listMyReports(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(reportService.listMine(userId));
    }

    @GetMapping("/stray-animals/nearby")
    public Result<List<StrayAnimalDistanceVO>> nearby(
            @RequestParam BigDecimal longitude,
            @RequestParam BigDecimal latitude,
            @RequestParam(required = false) Integer limit,
            @RequestHeader(value = "X-User-Id", required = false) Long viewerId) {
        return Result.success(animalService.listNearby(longitude, latitude, limit, viewerId));
    }

    @GetMapping("/stray-animals/{id}")
    public Result<StrayAnimalDetailVO> detail(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long viewerId) {
        return Result.success(animalService.getDetail(id, viewerId));
    }

    @PostMapping("/stray-adoption-applications")
    public Result<Void> submitAdoption(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody StrayAdoptionCreateDTO dto) {
        adoptionService.submit(userId, dto);
        return Result.success(null);
    }

    @GetMapping("/stray-adoption-applications/my")
    public Result<List<StrayAdoptionApplication>> listMyAdoptions(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(adoptionService.listMine(userId));
    }

    @PutMapping("/stray-adoption-applications/{id}/cancel")
    public Result<Void> cancelAdoption(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        adoptionService.cancel(id, userId);
        return Result.success(null);
    }
}
