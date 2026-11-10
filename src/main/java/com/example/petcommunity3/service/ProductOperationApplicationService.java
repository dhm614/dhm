package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.ApplicationReviewDTO;
import com.example.petcommunity3.dto.ProductCreateDTO;
import com.example.petcommunity3.dto.ProductUpdateDTO;
import com.example.petcommunity3.entity.Product;
import com.example.petcommunity3.entity.ProductOperationApplication;

import java.util.List;

public interface ProductOperationApplicationService {
    void submitCreate(Long merchantUserId, ProductCreateDTO dto);

    void submitUpdate(Long merchantUserId, Long productId, ProductUpdateDTO dto);

    void submitEnable(Long merchantUserId, Long productId);

    void submitDisable(Long merchantUserId, Long productId);

    List<Product> listMerchantProducts(Long merchantUserId);

    List<ProductOperationApplication> listMerchantApplications(Long merchantUserId);

    List<ProductOperationApplication> listAdminApplications(Integer status);

    void review(Long applicationId, Long reviewerId, ApplicationReviewDTO dto);
}
