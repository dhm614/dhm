package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.PetMapper;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.UserFollowMapper;
import com.example.petcommunity3.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileContentTypeTest {
    @Test
    void profilePostTotalsAndRecentPostsQueryApprovedNormalPostsOnly() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(PostMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, Post.class);
        UserMapper userMapper = mock(UserMapper.class);
        PostMapper postMapper = mock(PostMapper.class);
        PetMapper petMapper = mock(PetMapper.class);
        UserFollowMapper followMapper = mock(UserFollowMapper.class);
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(postMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(petMapper.selectList(any())).thenReturn(List.of());
        when(followMapper.selectCount(any())).thenReturn(0L);
        UserServiceImpl service = new UserServiceImpl(userMapper, petMapper, postMapper, followMapper);

        service.getProfile(1L, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Post>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper, org.mockito.Mockito.times(2)).selectList(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(wrapper -> {
            wrapper.getSqlSegment();
            assertThat(wrapper.getParamNameValuePairs())
                    .containsValue(1L)
                    .containsValue(1)
                    .containsValue(Post.CONTENT_TYPE_POST);
        });
    }
}
