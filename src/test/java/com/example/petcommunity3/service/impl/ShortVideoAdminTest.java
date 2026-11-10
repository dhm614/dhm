package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.petcommunity3.dto.StatusUpdateDTO;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.mapper.AppointmentMapper;
import com.example.petcommunity3.mapper.MerchantApplicationMapper;
import com.example.petcommunity3.mapper.PostCommentMapper;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.ProductOrderMapper;
import com.example.petcommunity3.mapper.TopicMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.NotificationService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortVideoAdminTest {
    @Mock
    private PostMapper postMapper;

    @Mock
    private NotificationService notificationService;

    private AdminServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(PostMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, Post.class);
        service = new AdminServiceImpl(
                org.mockito.Mockito.mock(UserMapper.class),
                postMapper,
                org.mockito.Mockito.mock(PostCommentMapper.class),
                org.mockito.Mockito.mock(AppointmentMapper.class),
                org.mockito.Mockito.mock(ProductOrderMapper.class),
                notificationService,
                org.mockito.Mockito.mock(TopicMapper.class),
                org.mockito.Mockito.mock(MerchantApplicationMapper.class)
        );
    }

    @Test
    void normalPostAuditListFiltersOutVideos() {
        service.listPosts(null, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Post>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper).selectList(captor.capture());
        captor.getValue().getSqlSegment();
        assertThat(captor.getValue().getParamNameValuePairs())
                .containsValue(0)
                .containsValue(Post.CONTENT_TYPE_POST);
    }

    @Test
    void videoAuditListDefaultsToPendingVideos() {
        service.listVideos(null, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Post>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper).selectList(captor.capture());
        captor.getValue().getSqlSegment();
        assertThat(captor.getValue().getParamNameValuePairs())
                .containsValue(0)
                .containsValue(Post.CONTENT_TYPE_VIDEO);
    }

    @Test
    void videoAuditDetailReturnsOnlyVideoContent() {
        Post video = video(6L, 0);
        when(postMapper.selectById(6L)).thenReturn(video);

        assertThat(service.getVideoDetail(6L)).isSameAs(video);

        Post normalPost = video(7L, 0);
        normalPost.setContentType(Post.CONTENT_TYPE_POST);
        when(postMapper.selectById(7L)).thenReturn(normalPost);

        assertThatThrownBy(() -> service.getVideoDetail(7L)).hasMessage("短视频不存在");
    }

    @Test
    void reviewVideoApprovesVideoAndSendsVideoNotification() {
        Post video = video(7L, 0);
        when(postMapper.selectById(7L)).thenReturn(video);
        StatusUpdateDTO dto = new StatusUpdateDTO();
        dto.setStatus(1);

        service.reviewVideo(7L, dto);

        assertThat(video.getStatus()).isEqualTo(1);
        verify(postMapper).updateById(video);
        verify(notificationService).create(
                eq(1L), isNull(), eq("CONTENT_REVIEW"), contains("短视频"), eq("POST"), eq(7L)
        );
    }

    @Test
    void reviewVideoRejectsNormalPost() {
        Post post = video(8L, 0);
        post.setContentType(Post.CONTENT_TYPE_POST);
        when(postMapper.selectById(8L)).thenReturn(post);
        StatusUpdateDTO dto = new StatusUpdateDTO();
        dto.setStatus(1);

        assertThatThrownBy(() -> service.reviewVideo(8L, dto)).hasMessage("短视频不存在");

        verify(postMapper, never()).updateById(any(Post.class));
    }

    @Test
    void deleteVideoHidesVideoAndClearsPostOperations() {
        Post video = video(9L, 1);
        video.setIsPinned(1);
        video.setIsFeatured(1);
        video.setPinnedTime(LocalDateTime.now());
        when(postMapper.selectById(9L)).thenReturn(video);

        service.deleteVideo(9L);

        assertThat(video.getStatus()).isEqualTo(3);
        assertThat(video.getIsPinned()).isZero();
        assertThat(video.getIsFeatured()).isZero();
        assertThat(video.getPinnedTime()).isNull();
        verify(postMapper).updateById(video);
    }

    @Test
    void restoreVideoApprovesVideo() {
        Post video = video(10L, 2);
        when(postMapper.selectById(10L)).thenReturn(video);

        service.restoreVideo(10L);

        assertThat(video.getStatus()).isEqualTo(1);
        verify(postMapper).updateById(video);
    }

    private Post video(Long id, Integer status) {
        Post post = new Post();
        post.setId(id);
        post.setUserId(1L);
        post.setTitle("猫咪短视频");
        post.setContentType(Post.CONTENT_TYPE_VIDEO);
        post.setStatus(status);
        post.setIsPinned(0);
        post.setIsFeatured(0);
        return post;
    }
}
