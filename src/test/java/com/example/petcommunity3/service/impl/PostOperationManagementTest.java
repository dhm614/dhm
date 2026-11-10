package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.service.SensitiveWordService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostOperationManagementTest {

    @Test
    void pinAndFeatureOnlyApprovedPost() {
        PostMapper postMapper = mock(PostMapper.class);
        AdminServiceImpl service = adminService(postMapper);
        Post approved = post(5L, 1);
        when(postMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(approved);

        service.pinPost(5L);
        service.featurePost(5L);

        assertThat(approved.getIsPinned()).isEqualTo(1);
        assertThat(approved.getPinnedTime()).isNotNull();
        assertThat(approved.getIsFeatured()).isEqualTo(1);

        Post pending = post(6L, 0);
        when(postMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pending);
        assertThatThrownBy(() -> service.pinPost(6L)).hasMessage("只有已审核帖子可以置顶或加精");
    }

    @Test
    void disablingPostMarksItBannedAndClearsOperationFlags() {
        PostMapper postMapper = mock(PostMapper.class);
        AdminServiceImpl service = adminService(postMapper);
        Post post = post(5L, 1);
        post.setIsPinned(1);
        post.setIsFeatured(1);
        post.setPinnedTime(java.time.LocalDateTime.now());
        when(postMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(post);

        service.disablePost(5L);

        assertThat(post.getStatus()).isEqualTo(3);
        assertThat(post.getIsPinned()).isZero();
        assertThat(post.getIsFeatured()).isZero();
        assertThat(post.getPinnedTime()).isNull();
    }

    @Test
    void publicPostListOrdersPinnedPostsFirst() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(PostMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, Post.class);
        PostMapper postMapper = mock(PostMapper.class);
        PostServiceImpl service = new PostServiceImpl(
                postMapper,
                mock(com.example.petcommunity3.mapper.TopicMapper.class),
                mock(SensitiveWordService.class)
        );

        service.list();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Post>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("is_pinned").contains("pinned_time");
    }

    private AdminServiceImpl adminService(PostMapper postMapper) {
        return new AdminServiceImpl(
                mock(com.example.petcommunity3.mapper.UserMapper.class),
                postMapper,
                mock(com.example.petcommunity3.mapper.PostCommentMapper.class),
                mock(com.example.petcommunity3.mapper.AppointmentMapper.class),
                mock(com.example.petcommunity3.mapper.ProductOrderMapper.class),
                mock(com.example.petcommunity3.service.NotificationService.class),
                mock(com.example.petcommunity3.mapper.TopicMapper.class),
                mock(com.example.petcommunity3.mapper.MerchantApplicationMapper.class)
        );
    }

    private Post post(Long id, Integer status) {
        Post post = new Post();
        post.setId(id);
        post.setUserId(1L);
        post.setTitle("测试帖子");
        post.setContentType(Post.CONTENT_TYPE_POST);
        post.setStatus(status);
        post.setIsPinned(0);
        post.setIsFeatured(0);
        return post;
    }
}
