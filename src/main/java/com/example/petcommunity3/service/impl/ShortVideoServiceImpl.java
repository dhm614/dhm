package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.ShortVideoCreateDTO;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.Topic;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.TopicMapper;
import com.example.petcommunity3.service.FileStorageService;
import com.example.petcommunity3.service.MerchantPermissionService;
import com.example.petcommunity3.service.SensitiveWordService;
import com.example.petcommunity3.service.ShortVideoService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
public class ShortVideoServiceImpl implements ShortVideoService {
    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 12;

    private final PostMapper postMapper;
    private final TopicMapper topicMapper;
    private final SensitiveWordService sensitiveWordService;
    private final MerchantPermissionService merchantPermissionService;
    private final FileStorageService fileStorageService;

    public ShortVideoServiceImpl(PostMapper postMapper,
                                 TopicMapper topicMapper,
                                 SensitiveWordService sensitiveWordService,
                                 MerchantPermissionService merchantPermissionService,
                                 FileStorageService fileStorageService) {
        this.postMapper = postMapper;
        this.topicMapper = topicMapper;
        this.sensitiveWordService = sensitiveWordService;
        this.merchantPermissionService = merchantPermissionService;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void create(ShortVideoCreateDTO dto) {
        if (dto == null) {
            throw new RuntimeException("视频信息不能为空");
        }
        if (dto.getUserId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        merchantPermissionService.requireActiveUser(dto.getUserId());
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new RuntimeException("视频标题不能为空");
        }
        if (!StringUtils.hasText(dto.getContent())) {
            throw new RuntimeException("视频简介不能为空");
        }
        if (!StringUtils.hasText(dto.getCover())) {
            throw new RuntimeException("封面URL不能为空");
        }
        if (!StringUtils.hasText(dto.getVideoUrl())) {
            throw new RuntimeException("视频URL不能为空");
        }
        if (!dto.getVideoUrl().endsWith(".mp4")) {
            throw new RuntimeException("视频仅支持MP4格式");
        }
        fileStorageService.validateManagedUrl(dto.getVideoUrl(), "short-video", Set.of(".mp4"));
        fileStorageService.validateManagedUrl(
                dto.getCover(),
                "short-video-cover",
                Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp")
        );

        if (StringUtils.hasText(dto.getTopic())) {
            Topic topic = topicMapper.selectOne(
                    new LambdaQueryWrapper<Topic>()
                            .eq(Topic::getName, dto.getTopic())
                            .eq(Topic::getStatus, 1)
            );
            if (topic == null) {
                throw new RuntimeException("话题不存在");
            }
        }

        sensitiveWordService.checkPost(dto.getTitle(), dto.getContent());

        Post post = new Post();
        post.setUserId(dto.getUserId());
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setCover(dto.getCover());
        post.setTopic(dto.getTopic());
        post.setContentType(Post.CONTENT_TYPE_VIDEO);
        post.setVideoUrl(dto.getVideoUrl());
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCollectCount(0);
        post.setIsPinned(0);
        post.setIsFeatured(0);
        post.setStatus(0);
        postMapper.insert(post);
    }

    @Override
    public List<Post> list(Integer limit) {
        int queryLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        return postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getContentType, Post.CONTENT_TYPE_VIDEO)
                        .eq(Post::getStatus, 1)
                        .orderByDesc(Post::getCreateTime)
                        .last("LIMIT " + queryLimit)
        );
    }

    @Override
    public List<Post> listByUserId(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        merchantPermissionService.requireActiveUser(userId);
        return postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getUserId, userId)
                        .eq(Post::getContentType, Post.CONTENT_TYPE_VIDEO)
                        .orderByDesc(Post::getCreateTime)
        );
    }

    @Override
    public Post getDetail(Long id) {
        if (id == null) {
            throw new RuntimeException("视频ID不能为空");
        }

        Post post = postMapper.selectOne(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getId, id)
                        .eq(Post::getContentType, Post.CONTENT_TYPE_VIDEO)
                        .eq(Post::getStatus, 1)
        );
        if (post == null) {
            throw new RuntimeException("视频不存在");
        }
        return post;
    }
}
