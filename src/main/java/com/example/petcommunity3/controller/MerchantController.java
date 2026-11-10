package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.ApplicationReviewDTO;
import com.example.petcommunity3.dto.MerchantApplicationCreateDTO;
import com.example.petcommunity3.entity.MerchantApplication;
import com.example.petcommunity3.service.AdminService;
import com.example.petcommunity3.service.MerchantApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MerchantController {
    private static final String USER_ID_HEADER = "X-User-Id";

    private final MerchantApplicationService merchantApplicationService;
    private final AdminService adminService;

    public MerchantController(MerchantApplicationService merchantApplicationService,
                              AdminService adminService) {
        this.merchantApplicationService = merchantApplicationService;
        this.adminService = adminService;
    }

    @PostMapping("/merchant/applications")
    public Result<Void> submit(@RequestHeader(value = USER_ID_HEADER, required = false) Long userId,
                               @RequestBody MerchantApplicationCreateDTO dto) {
        merchantApplicationService.submit(userId, dto);
        return Result.success(null);
    }

    @GetMapping("/merchant/applications/my")
    public Result<MerchantApplication> myApplication(
            @RequestHeader(value = USER_ID_HEADER, required = false) Long userId) {
        return Result.success(merchantApplicationService.getMyLatest(userId));
    }

    @GetMapping("/admin/merchant-applications")
    public Result<List<MerchantApplication>> list(
            @RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
            @RequestParam(required = false) Integer status) {
        adminService.requireAdmin(operatorId);
        return Result.success(merchantApplicationService.list(status));
    }

    @PutMapping("/admin/merchant-applications/{id}/review")
    public Result<Void> review(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                               @PathVariable Long id,
                               @RequestBody ApplicationReviewDTO dto) {
        adminService.requireAdmin(operatorId);
        merchantApplicationService.review(id, operatorId, dto);
        return Result.success(null);
    }
}
