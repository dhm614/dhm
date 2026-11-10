package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.ApplicationReviewDTO;
import com.example.petcommunity3.dto.ProductCreateDTO;
import com.example.petcommunity3.dto.ProductUpdateDTO;
import com.example.petcommunity3.entity.Product;
import com.example.petcommunity3.entity.ProductOperationApplication;
import com.example.petcommunity3.service.AdminService;
import com.example.petcommunity3.service.ProductOperationApplicationService;
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
public class MerchantProductController {
    private static final String USER_ID_HEADER = "X-User-Id";

    private final ProductOperationApplicationService applicationService;
    private final AdminService adminService;

    public MerchantProductController(ProductOperationApplicationService applicationService, AdminService adminService) {
        this.applicationService = applicationService;
        this.adminService = adminService;
    }

    @PostMapping("/merchant/product-applications")
    public Result<Void> create(@RequestHeader(value = USER_ID_HEADER, required = false) Long merchantUserId,
                               @RequestBody ProductCreateDTO dto) {
        applicationService.submitCreate(merchantUserId, dto);
        return Result.success(null);
    }

    @PutMapping("/merchant/product-applications/{productId}")
    public Result<Void> update(@RequestHeader(value = USER_ID_HEADER, required = false) Long merchantUserId,
                               @PathVariable Long productId,
                               @RequestBody ProductUpdateDTO dto) {
        applicationService.submitUpdate(merchantUserId, productId, dto);
        return Result.success(null);
    }

    @PostMapping("/merchant/product-applications/{productId}/enable")
    public Result<Void> enable(@RequestHeader(value = USER_ID_HEADER, required = false) Long merchantUserId,
                               @PathVariable Long productId) {
        applicationService.submitEnable(merchantUserId, productId);
        return Result.success(null);
    }

    @PostMapping("/merchant/product-applications/{productId}/disable")
    public Result<Void> disable(@RequestHeader(value = USER_ID_HEADER, required = false) Long merchantUserId,
                                @PathVariable Long productId) {
        applicationService.submitDisable(merchantUserId, productId);
        return Result.success(null);
    }

    @GetMapping("/merchant/products")
    public Result<List<Product>> merchantProducts(
            @RequestHeader(value = USER_ID_HEADER, required = false) Long merchantUserId) {
        return Result.success(applicationService.listMerchantProducts(merchantUserId));
    }

    @GetMapping("/merchant/product-applications")
    public Result<List<ProductOperationApplication>> merchantApplications(
            @RequestHeader(value = USER_ID_HEADER, required = false) Long merchantUserId) {
        return Result.success(applicationService.listMerchantApplications(merchantUserId));
    }

    @GetMapping("/admin/product-applications")
    public Result<List<ProductOperationApplication>> adminApplications(
            @RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
            @RequestParam(required = false) Integer status) {
        adminService.requireAdmin(operatorId);
        return Result.success(applicationService.listAdminApplications(status));
    }

    @PutMapping("/admin/product-applications/{id}/review")
    public Result<Void> review(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                               @PathVariable Long id,
                               @RequestBody ApplicationReviewDTO dto) {
        adminService.requireAdmin(operatorId);
        applicationService.review(id, operatorId, dto);
        return Result.success(null);
    }
}
