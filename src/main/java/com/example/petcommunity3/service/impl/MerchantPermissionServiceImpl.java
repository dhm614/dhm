package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.entity.Store;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.StoreMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.MerchantPermissionService;
import org.springframework.stereotype.Service;

@Service
public class MerchantPermissionServiceImpl implements MerchantPermissionService {
    private final UserMapper userMapper;
    private final StoreMapper storeMapper;

    public MerchantPermissionServiceImpl(UserMapper userMapper, StoreMapper storeMapper) {
        this.userMapper = userMapper;
        this.storeMapper = storeMapper;
    }

    @Override
    public User requireActiveUser(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户身份不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new RuntimeException("用户不存在或已被禁用");
        }
        return user;
    }

    @Override
    public User requireMerchantOrAdmin(Long userId) {
        User user = requireActiveUser(userId);
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return user;
        }
        if (!Integer.valueOf(1).equals(user.getIsMerchant())) {
            throw new RuntimeException("当前用户不是已审核商户");
        }
        return user;
    }

    @Override
    public void requireStoreOwnerOrAdmin(Long userId, Long storeId) {
        User user = requireMerchantOrAdmin(userId);
        if (storeId == null) {
            throw new RuntimeException("门店ID不能为空");
        }
        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new RuntimeException("门店不存在");
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return;
        }
        if (!userId.equals(store.getMerchantUserId())) {
            throw new RuntimeException("无权操作该商户数据");
        }
    }
}
