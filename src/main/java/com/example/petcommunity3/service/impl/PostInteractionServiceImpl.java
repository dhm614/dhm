package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.PostActionDTO;
import com.example.petcommunity3.dto.PostCommentCreateDTO;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.PostCollect;
import com.example.petcommunity3.entity.PostComment;
import com.example.petcommunity3.entity.PostLike;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.PostCollectMapper;
import com.example.petcommunity3.mapper.PostCommentMapper;
import com.example.petcommunity3.mapper.PostLikeMapper;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.NotificationService;
import com.example.petcommunity3.service.PostInteractionService;
import com.example.petcommunity3.service.SensitiveWordService;
import com.example.petcommunity3.vo.PostCommentTreeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostInteractionServiceImpl implements PostInteractionService {
    // PostInteractionServiceImpl 负责帖子互动模块：评论、点赞、取消点赞、收藏、取消收藏。
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final PostCommentMapper postCommentMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostCollectMapper postCollectMapper;
    private final SensitiveWordService sensitiveWordService;
    // 通知服务用于在别人评论、点赞、收藏你的帖子时生成消息提醒。
    private final NotificationService notificationService;

    public PostInteractionServiceImpl(PostMapper postMapper,
                                      UserMapper userMapper,
                                      PostCommentMapper postCommentMapper,
                                      PostLikeMapper postLikeMapper,
                                      PostCollectMapper postCollectMapper,
                                      SensitiveWordService sensitiveWordService,
                                      NotificationService notificationService) {
        // 构造方法注入多个 Mapper，因为互动会同时操作帖子表、评论表、点赞表和收藏表。
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.postCommentMapper = postCommentMapper;
        this.postLikeMapper = postLikeMapper;
        this.postCollectMapper = postCollectMapper;
        this.sensitiveWordService = sensitiveWordService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void comment(PostCommentCreateDTO dto) {
        // 评论流程：校验帖子和用户，检查评论内容，然后新增评论并增加帖子评论数。
        if (dto.getPostId() == null) {
            throw new RuntimeException("帖子ID不能为空");
        }

        if (dto.getUserId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        if (!StringUtils.hasText(dto.getContent())) {
            throw new RuntimeException("评论内容不能为空");
        }

        // getNormalPost 会确认帖子存在并且处于可展示状态。
        Post post = getNormalPost(dto.getPostId());
        sensitiveWordService.checkText(dto.getContent());

        // 把评论 DTO 转成 PostComment 实体，保存到评论表。
        PostComment comment = new PostComment();
        comment.setPostId(dto.getPostId());
        comment.setUserId(dto.getUserId());
        comment.setParentId(dto.getParentId());
        comment.setContent(dto.getContent());
        comment.setStatus(1);
        postCommentMapper.insert(comment);

        // 评论表新增一条后，帖子表里的冗余评论数也加 1，方便列表快速展示。
        post.setCommentCount(safeCount(post.getCommentCount()) + 1);
        postMapper.updateById(post);

        // 如果不是作者自己操作，就给帖子作者发一条通知。
        if (!post.getUserId().equals(dto.getUserId())) {
            notificationService.create(
                    post.getUserId(),
                    dto.getUserId(),
                    "COMMENT",
                    nickname(dto.getUserId()) + "评论了你的帖子：" + post.getTitle(),
                    "POST",
                    post.getId()
            );
        }
    }

    @Override
    public List<PostComment> listComments(Long postId) {
        // 查询评论列表：只取未删除 status=1 的评论，并按时间正序展示。
        if (postId == null) {
            throw new RuntimeException("帖子ID不能为空");
        }

        getNormalPost(postId);

        return postCommentMapper.selectList(
                new LambdaQueryWrapper<PostComment>()
                        .eq(PostComment::getPostId, postId)
                        .eq(PostComment::getStatus, 1)
                        .orderByAsc(PostComment::getCreateTime)
        );
    }

    @Override
    public List<PostCommentTreeVO> listCommentTree(Long postId) {
        List<PostComment> comments = listComments(postId);
        Map<Long, PostCommentTreeVO> commentMap = new LinkedHashMap<>();
        for (PostComment comment : comments) {
            commentMap.put(comment.getId(), PostCommentTreeVO.from(comment));
        }

        List<PostCommentTreeVO> roots = new ArrayList<>();
        for (PostComment comment : comments) {
            PostCommentTreeVO current = commentMap.get(comment.getId());
            Long parentId = comment.getParentId();
            if (parentId != null && commentMap.containsKey(parentId)) {
                commentMap.get(parentId).getChildren().add(current);
            } else {
                roots.add(current);
            }
        }
        return roots;
    }

    @Override
    @Transactional
    public void like(PostActionDTO dto) {
        // 点赞流程：检查参数和帖子状态，防止重复点赞，再写入点赞表。
        checkActionDTO(dto);

        // 先查点赞表，确认同一用户没有给同一帖子点过赞。
        PostLike existLike = postLikeMapper.selectOne(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getPostId, dto.getPostId())
                        .eq(PostLike::getUserId, dto.getUserId())
        );

        if (existLike != null) {
            throw new RuntimeException("不能重复点赞");
        }

        // 点赞关系本身是一条记录：post_id + user_id。
        PostLike like = new PostLike();
        like.setPostId(dto.getPostId());
        like.setUserId(dto.getUserId());
        postLikeMapper.insert(like);

        // getNormalPost 会确认帖子存在并且处于可展示状态。
        Post post = getNormalPost(dto.getPostId());
        post.setLikeCount(safeCount(post.getLikeCount()) + 1);
        postMapper.updateById(post);

        // 如果不是作者自己操作，就给帖子作者发一条通知。
        if (!post.getUserId().equals(dto.getUserId())) {
            notificationService.create(
                    post.getUserId(),
                    dto.getUserId(),
                    "LIKE",
                    nickname(dto.getUserId()) + "点赞了你的帖子：" + post.getTitle(),
                    "POST",
                    post.getId()
            );
        }
    }

    @Override
    @Transactional
    public void unlike(PostActionDTO dto) {
        // 取消点赞：必须先找到原来的点赞记录，再删除并减少帖子点赞数。
        checkActionDTO(dto);

        // 先查点赞表，确认同一用户没有给同一帖子点过赞。
        PostLike existLike = postLikeMapper.selectOne(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getPostId, dto.getPostId())
                        .eq(PostLike::getUserId, dto.getUserId())
        );

        if (existLike == null) {
            throw new RuntimeException("还没有点赞");
        }

        postLikeMapper.deleteById(existLike.getId());

        // getNormalPost 会确认帖子存在并且处于可展示状态。
        Post post = getNormalPost(dto.getPostId());
        post.setLikeCount(Math.max(safeCount(post.getLikeCount()) - 1, 0));
        postMapper.updateById(post);
    }

    @Override
    @Transactional
    public void collect(PostActionDTO dto) {
        // 收藏流程和点赞类似：先防重复，再写收藏表，并增加帖子收藏数。
        checkActionDTO(dto);

        PostCollect existCollect = postCollectMapper.selectOne(
                new LambdaQueryWrapper<PostCollect>()
                        .eq(PostCollect::getPostId, dto.getPostId())
                        .eq(PostCollect::getUserId, dto.getUserId())
        );

        if (existCollect != null) {
            throw new RuntimeException("不能重复收藏");
        }

        PostCollect collect = new PostCollect();
        collect.setPostId(dto.getPostId());
        collect.setUserId(dto.getUserId());
        postCollectMapper.insert(collect);

        // getNormalPost 会确认帖子存在并且处于可展示状态。
        Post post = getNormalPost(dto.getPostId());
        post.setCollectCount(safeCount(post.getCollectCount()) + 1);
        postMapper.updateById(post);

        // 如果不是作者自己操作，就给帖子作者发一条通知。
        if (!post.getUserId().equals(dto.getUserId())) {
            notificationService.create(
                    post.getUserId(),
                    dto.getUserId(),
                    "COLLECT",
                    nickname(dto.getUserId()) + "收藏了你的帖子：" + post.getTitle(),
                    "POST",
                    post.getId()
            );
        }
    }

    @Override
    @Transactional
    public void uncollect(PostActionDTO dto) {
        // 取消收藏：删除收藏关系，同时把帖子收藏数减 1。
        checkActionDTO(dto);

        PostCollect existCollect = postCollectMapper.selectOne(
                new LambdaQueryWrapper<PostCollect>()
                        .eq(PostCollect::getPostId, dto.getPostId())
                        .eq(PostCollect::getUserId, dto.getUserId())
        );

        if (existCollect == null) {
            throw new RuntimeException("还没有收藏");
        }

        postCollectMapper.deleteById(existCollect.getId());

        // getNormalPost 会确认帖子存在并且处于可展示状态。
        Post post = getNormalPost(dto.getPostId());
        post.setCollectCount(Math.max(safeCount(post.getCollectCount()) - 1, 0));
        postMapper.updateById(post);
    }

    private void checkActionDTO(PostActionDTO dto) {
        // 点赞和收藏共用的参数校验，减少重复代码。
        if (dto.getPostId() == null) {
            throw new RuntimeException("帖子ID不能为空");
        }

        if (dto.getUserId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        getNormalPost(dto.getPostId());
    }

    private Post getNormalPost(Long postId) {
        // 公共查询方法：只允许对存在且正常展示的帖子做互动。
        Post post = postMapper.selectById(postId);

        if (post == null || !Integer.valueOf(1).equals(post.getStatus())) {
            throw new RuntimeException("帖子不存在或已下架");
        }

        return post;
    }

    private String nickname(Long userId) {
        // 生成通知文案时优先使用昵称，没有昵称时再使用默认名称。
        User user = userMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getNickname())) {
            return "有人";
        }
        return user.getNickname();
    }

    private int safeCount(Integer count) {
        // 计数字段可能是 null，统一当作 0 处理，避免空指针。
        return count == null ? 0 : count;
    }
}
