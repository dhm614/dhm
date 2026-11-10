package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.LoginDTO;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.PetMapper;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.UserFollowMapper;
import com.example.petcommunity3.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserLoginStatusTest {

    @Test
    void disabledUserCannotLogin() {
        UserMapper userMapper = mock(UserMapper.class);
        UserServiceImpl service = new UserServiceImpl(
                userMapper,
                mock(PetMapper.class),
                mock(PostMapper.class),
                mock(UserFollowMapper.class)
        );
        User user = new User();
        user.setId(5L);
        user.setUsername("blocked-user");
        user.setStatus(0);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        LoginDTO dto = new LoginDTO();
        dto.setUsername("blocked-user");
        dto.setPassword("123456");

        assertThatThrownBy(() -> service.login(dto))
                .hasMessage("用户已被禁用");
    }
}
