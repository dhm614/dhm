package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.PostComment;
import com.example.petcommunity3.mapper.PostCollectMapper;
import com.example.petcommunity3.mapper.PostCommentMapper;
import com.example.petcommunity3.mapper.PostLikeMapper;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.NotificationService;
import com.example.petcommunity3.service.SensitiveWordService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostCommentVisibilityTest {
    @Test
    void pendingAndHiddenVideosRejectCommentReadsBeforeCommentQuery() {
        PostMapper postMapper = mock(PostMapper.class);
        PostCommentMapper commentMapper = mock(PostCommentMapper.class);
        PostInteractionServiceImpl service = createService(postMapper, commentMapper);
        when(postMapper.selectById(7L)).thenReturn(video(7L, 0));
        when(postMapper.selectById(8L)).thenReturn(video(8L, 2));

        assertThatThrownBy(() -> service.listComments(7L));
        assertThatThrownBy(() -> service.listComments(8L));

        verify(commentMapper, never()).selectList(any());
    }

    @Test
    void approvedVideoCommentsRemainReadable() {
        PostMapper postMapper = mock(PostMapper.class);
        PostCommentMapper commentMapper = mock(PostCommentMapper.class);
        PostInteractionServiceImpl service = createService(postMapper, commentMapper);
        PostComment comment = new PostComment();
        comment.setId(3L);
        comment.setPostId(9L);
        comment.setStatus(1);
        when(postMapper.selectById(9L)).thenReturn(video(9L, 1));
        when(commentMapper.selectList(any())).thenReturn(List.of(comment));

        assertThat(service.listComments(9L)).containsExactly(comment);
    }

    private PostInteractionServiceImpl createService(PostMapper postMapper, PostCommentMapper commentMapper) {
        return new PostInteractionServiceImpl(
                postMapper,
                mock(UserMapper.class),
                commentMapper,
                mock(PostLikeMapper.class),
                mock(PostCollectMapper.class),
                mock(SensitiveWordService.class),
                mock(NotificationService.class)
        );
    }

    private Post video(Long id, Integer status) {
        Post video = new Post();
        video.setId(id);
        video.setContentType(Post.CONTENT_TYPE_VIDEO);
        video.setStatus(status);
        return video;
    }
}
