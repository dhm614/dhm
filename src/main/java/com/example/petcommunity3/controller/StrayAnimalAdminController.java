package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.StrayAdoptionReviewDTO;
import com.example.petcommunity3.dto.StrayAnimalReportReviewDTO;
import com.example.petcommunity3.dto.StrayAnimalUpdateDTO;
import com.example.petcommunity3.entity.StrayAdoptionApplication;
import com.example.petcommunity3.entity.StrayAnimal;
import com.example.petcommunity3.entity.StrayAnimalReport;
import com.example.petcommunity3.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class StrayAnimalAdminController {
    private final AdminService adminService;
    private final StrayAnimalReportService reportService;
    private final StrayAnimalService animalService;
    private final StrayAdoptionService adoptionService;

    public StrayAnimalAdminController(AdminService adminService,
                                      StrayAnimalReportService reportService,
                                      StrayAnimalService animalService,
                                      StrayAdoptionService adoptionService) {
        this.adminService = adminService;
        this.reportService = reportService;
        this.animalService = animalService;
        this.adoptionService = adoptionService;
    }

    private void requireAdmin(Long userId) { adminService.requireAdmin(userId); }

    @GetMapping("/stray-animal-reports")
    public Result<List<StrayAnimalReport>> reports(@RequestHeader(value="X-User-Id", required=false) Long userId,
                                                    @RequestParam(required=false) Integer status) {
        requireAdmin(userId); return Result.success(reportService.listForAdmin(status));
    }

    @GetMapping("/stray-animal-reports/{id}")
    public Result<StrayAnimalReport> report(@RequestHeader(value="X-User-Id", required=false) Long userId,
                                             @PathVariable Long id) {
        requireAdmin(userId); return Result.success(reportService.getForAdmin(id));
    }

    @PutMapping("/stray-animal-reports/{id}/review")
    public Result<Void> reviewReport(@RequestHeader(value="X-User-Id", required=false) Long userId,
                                     @PathVariable Long id, @RequestBody StrayAnimalReportReviewDTO dto) {
        requireAdmin(userId); reportService.review(id, userId, dto); return Result.success(null);
    }

    @GetMapping("/stray-animals")
    public Result<List<StrayAnimal>> animals(@RequestHeader(value="X-User-Id", required=false) Long userId,
                                              @RequestParam(required=false) Integer rescueStatus) {
        requireAdmin(userId); return Result.success(animalService.listForAdmin(rescueStatus));
    }

    @PutMapping("/stray-animals/{id}")
    public Result<Void> updateAnimal(@RequestHeader(value="X-User-Id", required=false) Long userId,
                                     @PathVariable Long id, @RequestBody StrayAnimalUpdateDTO dto) {
        requireAdmin(userId); animalService.updateForAdmin(id, dto); return Result.success(null);
    }

    @GetMapping("/stray-adoption-applications")
    public Result<List<StrayAdoptionApplication>> adoptions(@RequestHeader(value="X-User-Id", required=false) Long userId,
                                                             @RequestParam(required=false) Integer status) {
        requireAdmin(userId); return Result.success(adoptionService.listForAdmin(status));
    }

    @PutMapping("/stray-adoption-applications/{id}/review")
    public Result<Void> reviewAdoption(@RequestHeader(value="X-User-Id", required=false) Long userId,
                                       @PathVariable Long id, @RequestBody StrayAdoptionReviewDTO dto) {
        requireAdmin(userId); adoptionService.review(id, userId, dto); return Result.success(null);
    }
}
