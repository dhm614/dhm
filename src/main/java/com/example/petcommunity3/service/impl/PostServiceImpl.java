package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.PostCreateDTO;
import com.example.petcommunity3.dto.PostUpdateDTO;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.Topic;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.TopicMapper;
import com.example.petcommunity3.service.PostService;
import com.example.petcommunity3.service.SensitiveWordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PostServiceImpl implements PostService {
    // PostServiceImpl 负责社区帖子模块：发帖、查询、修改、删除和按话题筛选。
    private final PostMapper postMapper;
    private final TopicMapper topicMapper;
    // 敏感词服务用于发帖和改帖前的内容审核。
    private final SensitiveWordService sensitiveWordService;

    public PostServiceImpl(PostMapper postMapper,
                           TopicMapper topicMapper,
                           SensitiveWordService sensitiveWordService) {
        // 构造方法注入 Mapper 和其它 Service，当前类只负责组织业务流程。
        this.postMapper = postMapper;
        this.topicMapper = topicMapper;
        this.sensitiveWordService = sensitiveWordService;
    }

    @Override
    @Transactional
    public void create(PostCreateDTO dto) {
        // 发帖流程：校验用户和内容，检查话题、敏感词，然后保存帖子。
        if (dto.getUserId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new RuntimeException("帖子标题不能为空");
        }
        if (!StringUtils.hasText(dto.getContent())) {
            throw new RuntimeException("帖子内容不能为空");
        }

        // 如果传了话题，必须确认话题存在且启用；随后检查标题和正文是否有敏感词。
        Topic topic = getEnabledTopic(dto.getTopic());
        sensitiveWordService.checkPost(dto.getTitle(), dto.getContent());

        // DTO 转成 Post 实体；计数类字段从 0 开始，新帖子先进入待审核状态。
        Post post = new Post();
        post.setUserId(dto.getUserId());
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setCover(dto.getCover());
        post.setTopic(dto.getTopic());
        post.setContentType(Post.CONTENT_TYPE_POST);
        post.setVideoUrl(null);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCollectCount(0);
        post.setIsPinned(0);
        post.setIsFeatured(0);
        post.setStatus(0);
        postMapper.insert(post);

        // 发帖成功后同步增加话题帖子数，保持话题列表里的冗余统计可用。
        if (topic != null) {
            topic.setPostCount((topic.getPostCount() == null ? 0 : topic.getPostCount()) + 1);
            topicMapper.updateById(topic);
        }
    }

    @Override
    public List<Post> list() {
        // 前台帖子列表只展示审核通过 status=1 的帖子，并按创建时间倒序排列。
        return postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .eq(Post::getContentType, Post.CONTENT_TYPE_POST)
                        .orderByDesc(Post::getIsPinned)
                        .orderByDesc(Post::getPinnedTime)
                        .orderByDesc(Post::getCreateTime)
        );
    }

    @Override
    public List<Post> listByUserId(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        return postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getUserId, userId)
                        .eq(Post::getContentType, Post.CONTENT_TYPE_POST)
                        .orderByDesc(Post::getCreateTime)
        );
    }

    @Override
    public Post getDetail(Long id) {
        // 查询帖子详情：先检查 ID，再确认帖子存在且状态允许展示。
        if (id == null) {
            throw new RuntimeException("帖子ID不能为空");
        }

        Post post = postMapper.selectOne(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getId, id)
                        .eq(Post::getStatus, 1)
                        .eq(Post::getContentType, Post.CONTENT_TYPE_POST)
        );
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        return post;
    }

    @Override
    @Transactional
    public void update(PostUpdateDTO dto) {
        // 修改帖子：先校验请求，再确认当前用户是帖子作者。
        if (dto.getId() == null) {
            throw new RuntimeException("帖子ID不能为空");
        }
        if (dto.getUserId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (StringUtils.hasText(dto.getTitle()) || StringUtils.hasText(dto.getContent())) {
            sensitiveWordService.checkPost(dto.getTitle(), dto.getContent());
        }
        // 修改话题时也要确认话题可用，避免帖子挂到不存在的话题下。
        getEnabledTopic(dto.getTopic());

        // 用帖子 ID + 用户 ID 查询，保证只能修改自己的帖子。
        Post post = postMapper.selectOne(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getId, dto.getId())
                        .eq(Post::getUserId, dto.getUserId())
                        .eq(Post::getContentType, Post.CONTENT_TYPE_POST)
        );
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }

        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setCover(dto.getCover());
        post.setTopic(dto.getTopic());
        postMapper.updateById(post);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        // 删除帖子：先确认帖子属于当前用户，再删除记录并维护话题统计。
        if (id == null) {
            throw new RuntimeException("帖子ID不能为空");
        }
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        // 用帖子 ID + 用户 ID 查询，保证只能修改自己的帖子。
        Post post = postMapper.selectOne(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getId, id)
                        .eq(Post::getUserId, userId)
                        .eq(Post::getContentType, Post.CONTENT_TYPE_POST)
        );
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }

        // deleteById 根据主键删除帖子记录。
        postMapper.deleteById(id);

        if (StringUtils.hasText(post.getTopic())) {
            Topic topic = topicMapper.selectOne(
                    new LambdaQueryWrapper<Topic>()
                            .eq(Topic::getName, post.getTopic())
            );
            // 发帖成功后同步增加话题帖子数，保持话题列表里的冗余统计可用。
        if (topic != null) {
                topic.setPostCount(Math.max((topic.getPostCount() == null ? 0 : topic.getPostCount()) - 1, 0));
                topicMapper.updateById(topic);
            }
        }
    }

    @Override
    public List<Post> listByTopic(String topic) {
        // 按话题查帖子，用于话题详情页或话题筛选列表。
        if (!StringUtils.hasText(topic)) {
            throw new RuntimeException("话题不能为空");
        }

        return postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getTopic, topic)
                        .eq(Post::getStatus, 1)
                        .eq(Post::getContentType, Post.CONTENT_TYPE_POST)
                        .orderByDesc(Post::getIsPinned)
                        .orderByDesc(Post::getPinnedTime)
                        .orderByDesc(Post::getCreateTime)
        );
    }

    @Override
    public void updateCover(Long id, Long userId, String cover) {
        // 单独更新封面图，只改变 cover 字段。
        if (id == null) {
            throw new RuntimeException("帖子ID不能为空");
        }
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (!StringUtils.hasText(cover)) {
            throw new RuntimeException("封面URL不能为空");
        }

        // 用帖子 ID + 用户 ID 查询，保证只能修改自己的帖子。
        Post post = postMapper.selectOne(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getId, id)
                        .eq(Post::getUserId, userId)
                        .eq(Post::getContentType, Post.CONTENT_TYPE_POST)
        );
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }

        post.setCover(cover);
        postMapper.updateById(post);
    }

    private Topic getEnabledTopic(String topicName) {
        // 话题为空表示不挂话题；不为空时必须查到启用中的话题。
        if (!StringUtils.hasText(topicName)) {
            return null;
        }

        Topic topic = topicMapper.selectOne(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getName, topicName)
                        .eq(Topic::getStatus, 1)
        );
        if (topic == null) {
            throw new RuntimeException("话题不存在");
        }
        return topic;
    }
}
