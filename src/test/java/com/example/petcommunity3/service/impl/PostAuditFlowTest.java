package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.petcommunity3.dto.PostCreateDTO;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.TopicMapper;
import com.example.petcommunity3.service.SensitiveWordService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostAuditFlowTest {

    @Test
    void postContentTypeConstantsUseExpectedValues() {
        assertThat(Post.CONTENT_TYPE_POST).isEqualTo("POST");
        assertThat(Post.CONTENT_TYPE_VIDEO).isEqualTo("VIDEO");
    }

    @Test
    void newPostsAreCreatedAsPendingReview() {
        PostMapper postMapper = mock(PostMapper.class);
        TopicMapper topicMapper = mock(TopicMapper.class);
        SensitiveWordService sensitiveWordService = mock(SensitiveWordService.class);
        PostServiceImpl service = new PostServiceImpl(postMapper, topicMapper, sensitiveWordService);

        PostCreateDTO dto = new PostCreateDTO();
        dto.setUserId(1L);
        dto.setTitle("待审核帖子");
        dto.setContent("发布后需要后台审核");

        when(postMapper.insert(any(Post.class))).thenReturn(1);

        service.create(dto);

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(postCaptor.capture());
        assertThat(postCaptor.getValue().getStatus()).isEqualTo(0);
        assertThat(postCaptor.getValue().getContentType()).isEqualTo(Post.CONTENT_TYPE_POST);
        assertThat(postCaptor.getValue().getVideoUrl()).isNull();
    }

    @Test
    void publicPostListOnlyQueriesApprovedNormalPosts() {
        initializePostTableInfo();
        PostMapper postMapper = mock(PostMapper.class);
        PostServiceImpl service = new PostServiceImpl(
                postMapper,
                mock(TopicMapper.class),
                mock(SensitiveWordService.class)
        );

        service.list();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Post>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper).selectList(wrapperCaptor.capture());
        wrapperCaptor.getValue().getSqlSegment();
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs()).containsValue(1).containsValue(Post.CONTENT_TYPE_POST);
    }

    @Test
    void adminAuditPostListDefaultsToPendingPosts() {
        initializePostTableInfo();

        AdminServiceImpl service = new AdminServiceImpl(
                mock(com.example.petcommunity3.mapper.UserMapper.class),
                mock(PostMapper.class),
                mock(com.example.petcommunity3.mapper.PostCommentMapper.class),
                mock(com.example.petcommunity3.mapper.AppointmentMapper.class),
                mock(com.example.petcommunity3.mapper.ProductOrderMapper.class),
                mock(com.example.petcommunity3.service.NotificationService.class),
                mock(com.example.petcommunity3.mapper.TopicMapper.class),
                mock(com.example.petcommunity3.mapper.MerchantApplicationMapper.class)
        );
        PostMapper postMapper = (PostMapper) org.springframework.test.util.ReflectionTestUtils.getField(service, "postMapper");

        service.listPosts(null, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Post>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("status");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs()).containsValue(0);
    }

    @Test
    void adminPostDetailReturnsOnlyNormalPost() {
        PostMapper postMapper = mock(PostMapper.class);
        AdminServiceImpl service = new AdminServiceImpl(
                mock(com.example.petcommunity3.mapper.UserMapper.class),
                postMapper,
                mock(com.example.petcommunity3.mapper.PostCommentMapper.class),
                mock(com.example.petcommunity3.mapper.AppointmentMapper.class),
                mock(com.example.petcommunity3.mapper.ProductOrderMapper.class),
                mock(com.example.petcommunity3.service.NotificationService.class),
                mock(com.example.petcommunity3.mapper.TopicMapper.class),
                mock(com.example.petcommunity3.mapper.MerchantApplicationMapper.class)
        );
        Post post = new Post();
        post.setId(11L);
        post.setContentType(Post.CONTENT_TYPE_POST);
        when(postMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(post);

        assertThat(service.getPostDetail(11L)).isSameAs(post);

        when(postMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThatThrownBy(() -> service.getPostDetail(12L)).hasMessage("帖子不存在");
    }

    private void initializePostTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(PostMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, Post.class);
    }
}
