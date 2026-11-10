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
import com.example.petcommunity3.vo.PostCommentTreeVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostCommentTreeTest {

    @Test
    void listCommentTreeBuildsChildrenFromParentId() {
        PostCommentMapper commentMapper = mock(PostCommentMapper.class);
        PostMapper postMapper = mock(PostMapper.class);
        PostInteractionServiceImpl service = new PostInteractionServiceImpl(
                postMapper,
                mock(UserMapper.class),
                commentMapper,
                mock(PostLikeMapper.class),
                mock(PostCollectMapper.class),
                mock(SensitiveWordService.class),
                mock(NotificationService.class)
        );
        PostComment root = comment(1L, null, "一级评论");
        PostComment child = comment(2L, 1L, "回复一级评论");
        PostComment anotherRoot = comment(3L, null, "另一个一级评论");

        Post post = new Post();
        post.setId(9L);
        post.setStatus(1);
        when(postMapper.selectById(9L)).thenReturn(post);
        when(commentMapper.selectList(any())).thenReturn(List.of(root, child, anotherRoot));

        List<PostCommentTreeVO> tree = service.listCommentTree(9L);

        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).getId()).isEqualTo(1L);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getId()).isEqualTo(2L);
        assertThat(tree.get(1).getId()).isEqualTo(3L);
    }

    private PostComment comment(Long id, Long parentId, String content) {
        PostComment comment = new PostComment();
        comment.setId(id);
        comment.setPostId(9L);
        comment.setUserId(id);
        comment.setParentId(parentId);
        comment.setContent(content);
        comment.setStatus(1);
        comment.setCreateTime(LocalDateTime.now().plusSeconds(id));
        return comment;
    }
}
