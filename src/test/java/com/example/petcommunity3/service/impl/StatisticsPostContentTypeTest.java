package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.mapper.AppointmentMapper;
import com.example.petcommunity3.mapper.PetMapper;
import com.example.petcommunity3.mapper.PostCommentMapper;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.ProductMapper;
import com.example.petcommunity3.mapper.ProductOrderMapper;
import com.example.petcommunity3.mapper.TopicMapper;
import com.example.petcommunity3.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StatisticsPostContentTypeTest {
    private PostMapper postMapper;
    private StatisticsServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(PostMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, Post.class);
        postMapper = mock(PostMapper.class);
        service = new StatisticsServiceImpl(
                mock(UserMapper.class),
                mock(PetMapper.class),
                postMapper,
                mock(PostCommentMapper.class),
                mock(AppointmentMapper.class),
                mock(ProductMapper.class),
                mock(ProductOrderMapper.class),
                mock(TopicMapper.class)
        );
    }

    @Test
    void overviewPostCountQueriesApprovedNormalPostsOnly() {
        service.overview();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Post>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper).selectCount(captor.capture());
        captor.getValue().getSqlSegment();
        assertThat(captor.getValue().getParamNameValuePairs())
                .containsValue(1)
                .containsValue(Post.CONTENT_TYPE_POST);
    }

    @Test
    void enhancedTodayTrendActiveAndHotPostQueriesUseNormalPostsOnly() {
        service.enhanced(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Post>> countCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper, org.mockito.Mockito.times(2)).selectCount(countCaptor.capture());
        assertNormalPostFilters(countCaptor);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Post>> listCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper, org.mockito.Mockito.times(2)).selectList(listCaptor.capture());
        assertNormalPostFilters(listCaptor);
    }

    private void assertNormalPostFilters(ArgumentCaptor<LambdaQueryWrapper<Post>> captor) {
        assertThat(captor.getAllValues()).allSatisfy(wrapper -> {
            wrapper.getSqlSegment();
            assertThat(wrapper.getParamNameValuePairs()).containsValue(Post.CONTENT_TYPE_POST);
        });
    }
}
