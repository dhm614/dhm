package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.entity.MerchantApplication;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.AppointmentMapper;
import com.example.petcommunity3.mapper.MerchantApplicationMapper;
import com.example.petcommunity3.mapper.PostCommentMapper;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.ProductOrderMapper;
import com.example.petcommunity3.mapper.TopicMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserManagementTest {

    @Test
    void listUsersByIdHidesPassword() {
        UserMapper userMapper = mock(UserMapper.class);
        AdminServiceImpl service = adminService(userMapper, mock(MerchantApplicationMapper.class));
        User user = user(5L, "USER", 0, 1);
        user.setPassword("secret");
        when(userMapper.selectById(5L)).thenReturn(user);

        List<User> users = service.listUsers(5L);

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getId()).isEqualTo(5L);
        assertThat(users.get(0).getPassword()).isNull();
    }

    @Test
    void setMerchantUserUpdatesFlagsAndApprovesPendingApplication() {
        UserMapper userMapper = mock(UserMapper.class);
        MerchantApplicationMapper applicationMapper = mock(MerchantApplicationMapper.class);
        AdminServiceImpl service = adminService(userMapper, applicationMapper);
        User user = user(5L, "USER", 0, 1);
        MerchantApplication application = new MerchantApplication();
        application.setId(8L);
        application.setUserId(5L);
        application.setStatus(0);
        when(userMapper.selectById(5L)).thenReturn(user);
        when(applicationMapper.selectList(any())).thenReturn(List.of(application));

        service.setMerchantUser(5L, 3L);

        assertThat(user.getIsMerchant()).isEqualTo(1);
        assertThat(user.getMerchantApplyStatus()).isEqualTo(2);
        assertThat(application.getStatus()).isEqualTo(1);
        assertThat(application.getReviewerId()).isEqualTo(3L);
        verify(userMapper).updateById(user);
        verify(applicationMapper).updateById(application);
    }

    @Test
    void setNormalUserClearsMerchantFlags() {
        UserMapper userMapper = mock(UserMapper.class);
        MerchantApplicationMapper applicationMapper = mock(MerchantApplicationMapper.class);
        AdminServiceImpl service = adminService(userMapper, applicationMapper);
        User user = user(5L, "USER", 1, 1);
        user.setMerchantApplyStatus(2);
        when(userMapper.selectById(5L)).thenReturn(user);
        when(applicationMapper.selectList(any())).thenReturn(Collections.emptyList());

        service.setNormalUser(5L, 3L);

        assertThat(user.getIsMerchant()).isZero();
        assertThat(user.getMerchantApplyStatus()).isZero();
        verify(userMapper).updateById(user);
    }

    @Test
    void adminAccountCannotBeChangedOrDisabled() {
        UserMapper userMapper = mock(UserMapper.class);
        AdminServiceImpl service = adminService(userMapper, mock(MerchantApplicationMapper.class));
        User admin = user(3L, "ADMIN", 0, 1);
        when(userMapper.selectById(3L)).thenReturn(admin);

        assertThatThrownBy(() -> service.setMerchantUser(3L, 3L))
                .hasMessage("管理员账号不能修改用户类型");
        assertThatThrownBy(() -> service.disableUser(3L))
                .hasMessage("管理员账号不能被封禁");
    }

    private AdminServiceImpl adminService(UserMapper userMapper,
                                          MerchantApplicationMapper applicationMapper) {
        return new AdminServiceImpl(
                userMapper,
                mock(PostMapper.class),
                mock(PostCommentMapper.class),
                mock(AppointmentMapper.class),
                mock(ProductOrderMapper.class),
                mock(NotificationService.class),
                mock(TopicMapper.class),
                applicationMapper
        );
    }

    private User user(Long id, String role, Integer isMerchant, Integer status) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setIsMerchant(isMerchant);
        user.setMerchantApplyStatus(isMerchant == 1 ? 2 : 0);
        user.setStatus(status);
        return user;
    }
}
