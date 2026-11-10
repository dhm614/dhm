package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.entity.Store;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.StoreMapper;
import com.example.petcommunity3.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MerchantOwnershipAuthorizationTest {

    @Test
    void normalUserCannotUseMerchantWriteOperations() {
        UserMapper userMapper = mock(UserMapper.class);
        MerchantPermissionServiceImpl service = new MerchantPermissionServiceImpl(userMapper, mock(StoreMapper.class));
        when(userMapper.selectById(1L)).thenReturn(user(1L, "USER", 0));

        assertThatThrownBy(() -> service.requireMerchantOrAdmin(1L))
                .hasMessage("当前用户不是已审核商户");
    }

    @Test
    void approvedMerchantCanUseMerchantWriteOperations() {
        UserMapper userMapper = mock(UserMapper.class);
        MerchantPermissionServiceImpl service = new MerchantPermissionServiceImpl(userMapper, mock(StoreMapper.class));
        User merchant = user(2L, "USER", 1);
        when(userMapper.selectById(2L)).thenReturn(merchant);

        assertThat(service.requireMerchantOrAdmin(2L)).isSameAs(merchant);
    }

    @Test
    void merchantCannotOperateAnotherMerchantsStore() {
        UserMapper userMapper = mock(UserMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        MerchantPermissionServiceImpl service = new MerchantPermissionServiceImpl(userMapper, storeMapper);
        when(userMapper.selectById(2L)).thenReturn(user(2L, "USER", 1));
        Store store = new Store();
        store.setId(9L);
        store.setMerchantUserId(7L);
        when(storeMapper.selectById(9L)).thenReturn(store);

        assertThatThrownBy(() -> service.requireStoreOwnerOrAdmin(2L, 9L))
                .hasMessage("无权操作该商户数据");
    }

    private User user(Long id, String role, Integer isMerchant) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(1);
        user.setIsMerchant(isMerchant);
        return user;
    }
}
