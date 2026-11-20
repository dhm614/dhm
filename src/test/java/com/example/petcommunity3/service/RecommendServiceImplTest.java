package com.example.petcommunity3.service;

import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.RecommendMapper;
import com.example.petcommunity3.mapper.UserBehaviorMapper;
import com.example.petcommunity3.mapper.UserInterestTagMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.impl.RecommendServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 召回与多样性重排的行为测试：用 mock 的 Mapper 喂固定数据，验证排序规则。
 */
@ExtendWith(MockitoExtension.class)
class RecommendServiceImplTest {

    @Mock
    private RecommendMapper recommendMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserBehaviorMapper behaviorMapper;
    @Mock
    private UserInterestTagMapper interestTagMapper;
    @Mock
    private RecommendConfigService configService;

    private RecommendServiceImpl service;

    @BeforeEach
    void setUp() {
        when(configService.getDouble(anyString(), anyDouble())).thenAnswer(inv -> inv.getArgument(1));
        when(configService.getInt(anyString(), anyInt())).thenAnswer(inv -> inv.getArgument(1));
        service = new RecommendServiceImpl(recommendMapper, postMapper, userMapper,
                behaviorMapper, interestTagMapper, new RecommendScorer(), configService);
    }

    @Test
    void shouldCapVideosPerSameAuthor() {
        LocalDateTime now = LocalDateTime.now();
        List<Post> posts = List.of(
                video(1L, 1L, now),
                video(2L, 1L, now.minusHours(1)),
                video(3L, 1L, now.minusHours(2)),
                video(4L, 2L, now.minusHours(3)));

        when(recommendMapper.selectHot(anyInt())).thenReturn(posts);
        when(recommendMapper.selectFresh(anyInt())).thenReturn(List.of());
        when(recommendMapper.selectLatest(anyInt())).thenReturn(List.of());
        when(recommendMapper.selectStatsByIds(anyList())).thenReturn(List.of());
        when(postMapper.selectBatchIds(anyList())).thenReturn(posts);

        List<Long> ids = service.rankVideoIds(null, null, 10);

        // 同作者最多 2 条：第三条被挤到尾部，不同作者的第 4 条顶上来
        assertEquals(List.of(1L, 2L, 4L, 3L), ids);
    }

    @Test
    void shouldCapVideosPerSameTopic() {
        LocalDateTime now = LocalDateTime.now();
        List<Post> posts = List.of(
                videoWithTopic(1L, 1L, "新手养猫", now),
                videoWithTopic(2L, 2L, "新手养猫", now.minusHours(1)),
                videoWithTopic(3L, 3L, "新手养猫", now.minusHours(2)),
                videoWithTopic(4L, 4L, "新手养猫", now.minusHours(3)),
                videoWithTopic(5L, 5L, "狗狗训练", now.minusHours(4)));

        when(recommendMapper.selectHot(anyInt())).thenReturn(posts);
        when(recommendMapper.selectFresh(anyInt())).thenReturn(List.of());
        when(recommendMapper.selectLatest(anyInt())).thenReturn(List.of());
        when(recommendMapper.selectStatsByIds(anyList())).thenReturn(List.of());
        when(postMapper.selectBatchIds(anyList())).thenReturn(posts);

        List<Long> ids = service.rankVideoIds(null, null, 10);

        // 同话题上限 3：前三条保持，第四条同话题让位给别的话题
        assertEquals(List.of(1L, 2L, 3L, 5L, 4L), ids);
    }

    @Test
    void shouldDropExcludedVideosForEvaluation() {
        LocalDateTime now = LocalDateTime.now();
        List<Post> posts = List.of(
                video(1L, 1L, now),
                video(2L, 2L, now.minusHours(1)),
                video(3L, 3L, now.minusHours(2)));

        when(recommendMapper.selectHot(anyInt())).thenReturn(posts);
        when(recommendMapper.selectFresh(anyInt())).thenReturn(List.of());
        when(recommendMapper.selectLatest(anyInt())).thenReturn(List.of());
        when(recommendMapper.selectStatsByIds(anyList())).thenReturn(List.of());
        when(postMapper.selectBatchIds(anyList())).thenReturn(posts);

        List<Long> ids = service.rankVideoIds(null, List.of(2L), 10);

        assertEquals(List.of(1L, 3L), ids);
    }

    private static Post video(Long id, Long authorId, LocalDateTime createTime) {
        return videoWithTopic(id, authorId, null, createTime);
    }

    private static Post videoWithTopic(Long id, Long authorId, String topic, LocalDateTime createTime) {
        Post post = new Post();
        post.setId(id);
        post.setUserId(authorId);
        post.setTopic(topic);
        post.setContentType("VIDEO");
        post.setStatus(1);
        post.setTitle("视频" + id);
        post.setCreateTime(createTime);
        return post;
    }
}
