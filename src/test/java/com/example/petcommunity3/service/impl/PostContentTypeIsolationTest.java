package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.petcommunity3.dto.PostUpdateDTO;
import com.example.petcommunity3.dto.StatusUpdateDTO;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.TopicMapper;
import com.example.petcommunity3.service.SensitiveWordService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostContentTypeIsolationTest {
    @BeforeAll
    static void initializeTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(PostMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, Post.class);
    }

    @Test
    void userUpdateRejectsVideoIdAtPostLookupBoundary() {
        PostMapper mapper = videoAwarePostMapper();
        PostServiceImpl service = postService(mapper);
        PostUpdateDTO dto = new PostUpdateDTO();
        dto.setId(7L);
        dto.setUserId(1L);
        dto.setTitle("updated");
        dto.setContent("updated content");

        assertThatThrownBy(() -> service.update(dto));

        verify(mapper, never()).updateById(any(Post.class));
    }

    @Test
    void userDeleteRejectsVideoIdAtPostLookupBoundary() {
        PostMapper mapper = videoAwarePostMapper();
        PostServiceImpl service = postService(mapper);

        assertThatThrownBy(() -> service.delete(7L, 1L));

        verify(mapper, never()).deleteById(7L);
    }

    @Test
    void userCoverUpdateRejectsVideoIdAtPostLookupBoundary() {
        PostMapper mapper = videoAwarePostMapper();
        PostServiceImpl service = postService(mapper);

        assertThatThrownBy(() -> service.updateCover(7L, 1L, "/uploads/post/new.jpg"));

        verify(mapper, never()).updateById(any(Post.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listByUserReturnsAllOwnedPostsWithoutStatusFilter() {
        PostMapper mapper = mock(PostMapper.class);
        PostServiceImpl service = postService(mapper);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(java.util.List.of());

        service.listByUserId(4L);

        ArgumentCaptor<LambdaQueryWrapper<Post>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(captor.capture());
        LambdaQueryWrapper<Post> wrapper = captor.getValue();
        String sql = wrapper.getSqlSegment();
        assertThat(wrapper.getParamNameValuePairs())
                .containsValue(4L)
                .containsValue(Post.CONTENT_TYPE_POST)
                .doesNotContainValue(0)
                .doesNotContainValue(1)
                .doesNotContainValue(2)
                .doesNotContainValue(3);
        assertThat(sql).contains("create_time").contains("DESC");
    }

    @ParameterizedTest(name = "generic admin operation {0} rejects video")
    @MethodSource("adminPostOperations")
    void genericAdminPostOperationsRejectVideoIds(String name, Consumer<AdminServiceImpl> operation) {
        PostMapper mapper = mock(PostMapper.class);
        Post video = video();
        when(mapper.selectById(7L)).thenReturn(video);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        AdminServiceImpl service = adminService(mapper);

        assertThatThrownBy(() -> operation.accept(service));

        verify(mapper, never()).updateById(any(Post.class));
    }

    private static Stream<Arguments> adminPostOperations() {
        StatusUpdateDTO review = new StatusUpdateDTO();
        review.setStatus(1);
        return Stream.of(
                Arguments.of("disable", (Consumer<AdminServiceImpl>) service -> service.disablePost(7L)),
                Arguments.of("enable", (Consumer<AdminServiceImpl>) service -> service.enablePost(7L)),
                Arguments.of("pin", (Consumer<AdminServiceImpl>) service -> service.pinPost(7L)),
                Arguments.of("unpin", (Consumer<AdminServiceImpl>) service -> service.unpinPost(7L)),
                Arguments.of("feature", (Consumer<AdminServiceImpl>) service -> service.featurePost(7L)),
                Arguments.of("unfeature", (Consumer<AdminServiceImpl>) service -> service.unfeaturePost(7L)),
                Arguments.of("review", (Consumer<AdminServiceImpl>) service -> service.reviewPost(7L, review))
        );
    }

    @SuppressWarnings("unchecked")
    private PostMapper videoAwarePostMapper() {
        PostMapper mapper = mock(PostMapper.class);
        Post video = video();
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            LambdaQueryWrapper<Post> wrapper = invocation.getArgument(0);
            wrapper.getSqlSegment();
            return wrapper.getParamNameValuePairs().containsValue(Post.CONTENT_TYPE_POST) ? null : video;
        });
        return mapper;
    }

    private PostServiceImpl postService(PostMapper mapper) {
        return new PostServiceImpl(mapper, mock(TopicMapper.class), mock(SensitiveWordService.class));
    }

    private AdminServiceImpl adminService(PostMapper mapper) {
        return new AdminServiceImpl(
                mock(com.example.petcommunity3.mapper.UserMapper.class),
                mapper,
                mock(com.example.petcommunity3.mapper.PostCommentMapper.class),
                mock(com.example.petcommunity3.mapper.AppointmentMapper.class),
                mock(com.example.petcommunity3.mapper.ProductOrderMapper.class),
                mock(com.example.petcommunity3.service.NotificationService.class),
                mock(com.example.petcommunity3.mapper.TopicMapper.class),
                mock(com.example.petcommunity3.mapper.MerchantApplicationMapper.class)
        );
    }

    private static Post video() {
        Post video = new Post();
        video.setId(7L);
        video.setUserId(1L);
        video.setTitle("video");
        video.setContentType(Post.CONTENT_TYPE_VIDEO);
        video.setStatus(1);
        video.setIsPinned(0);
        video.setIsFeatured(0);
        return video;
    }
}
