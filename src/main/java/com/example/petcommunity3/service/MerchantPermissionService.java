package com.example.petcommunity3.service;

import com.example.petcommunity3.entity.User;

public interface MerchantPermissionService {
    User requireActiveUser(Long userId);

    User requireMerchantOrAdmin(Long userId);

    void requireStoreOwnerOrAdmin(Long userId, Long storeId);
}
