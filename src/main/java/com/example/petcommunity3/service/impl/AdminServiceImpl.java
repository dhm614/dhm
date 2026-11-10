package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.StatusUpdateDTO;
import com.example.petcommunity3.dto.TopicCreateDTO;
import com.example.petcommunity3.dto.TopicReviewDTO;
import com.example.petcommunity3.entity.Appointment;
import com.example.petcommunity3.entity.MerchantApplication;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.PostComment;
import com.example.petcommunity3.entity.ProductOrder;
import com.example.petcommunity3.entity.Topic;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.AppointmentMapper;
import com.example.petcommunity3.mapper.MerchantApplicationMapper;
import com.example.petcommunity3.mapper.PostCommentMapper;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.ProductOrderMapper;
import com.example.petcommunity3.mapper.TopicMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.AdminService;
import com.example.petcommunity3.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {
    // AdminServiceImpl 负责后台管理模块：权限校验、内容审核、用户封禁和后台列表查询。
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_HIDDEN = 2;
    private static final int STATUS_BANNED = 3;

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final PostCommentMapper postCommentMapper;
    private final AppointmentMapper appointmentMapper;
    private final ProductOrderMapper productOrderMapper;
    private final NotificationService notificationService;
    private final TopicMapper topicMapper;
    private final MerchantApplicationMapper merchantApplicationMapper;

    public AdminServiceImpl(UserMapper userMapper,
                            PostMapper postMapper,
                            PostCommentMapper postCommentMapper,
                            AppointmentMapper appointmentMapper,
                            ProductOrderMapper productOrderMapper,
                            NotificationService notificationService,
                            TopicMapper topicMapper,
                            MerchantApplicationMapper merchantApplicationMapper) {
        this.userMapper = userMapper;
        this.postMapper = postMapper;
        this.postCommentMapper = postCommentMapper;
        this.appointmentMapper = appointmentMapper;
        this.productOrderMapper = productOrderMapper;
        this.notificationService = notificationService;
        this.topicMapper = topicMapper;
        this.merchantApplicationMapper = merchantApplicationMapper;
    }

    @Override
    public void requireAdmin(Long operatorId) {
        // 管理员权限校验：后台接口调用前必须先确认操作者是 ADMIN。
        if (operatorId == null) {
            throw new RuntimeException("请先登录管理员账号");
        }
        User user = userMapper.selectById(operatorId);
        if (user == null
                || Integer.valueOf(0).equals(user.getStatus())
                || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("管理员权限不足");
        }
    }

    @Override
    public List<User> listUsers(Long id) {
        if (id != null) {
            User user = userMapper.selectById(id);
            if (user == null) {
                return Collections.emptyList();
            }
            clearPassword(user);
            return List.of(user);
        }
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().orderByDesc(User::getId)
        );
        users.forEach(this::clearPassword);
        return users;
    }

    @Override
    @Transactional
    public void setMerchantUser(Long id, Long operatorId) {
        User user = getManageableUser(id, "管理员账号不能修改用户类型");
        user.setRole("USER");
        user.setIsMerchant(1);
        user.setMerchantApplyStatus(2);
        userMapper.updateById(user);
        finishPendingMerchantApplications(id, operatorId, 1, "管理员直接调整为商户");
    }

    @Override
    @Transactional
    public void setNormalUser(Long id, Long operatorId) {
        User user = getManageableUser(id, "管理员账号不能修改用户类型");
        user.setRole("USER");
        user.setIsMerchant(0);
        user.setMerchantApplyStatus(0);
        userMapper.updateById(user);
        finishPendingMerchantApplications(id, operatorId, 2, "管理员调整为普通用户");
    }

    @Override
    public void disableUser(Long id) {
        // 禁用用户：把用户 status 改成 0。
        User user = getManageableUser(id, "管理员账号不能被封禁");
        user.setStatus(0);
        userMapper.updateById(user);
    }

    @Override
    public void enableUser(Long id) {
        // 启用用户：把用户 status 改成 1。
        User user = getUser(id);
        user.setStatus(1);
        userMapper.updateById(user);
    }

    @Override
    public void disablePost(Long id) {
        // 下架帖子：状态 3 与审核驳回状态 2 分开记录。
        Post post = getPost(id);
        post.setStatus(STATUS_BANNED);
        clearPostOperations(post);
        postMapper.updateById(post);
        notifyContentReview(post.getUserId(), "你的帖子已被下架：" + post.getTitle(), "POST", post.getId());
    }

    @Override
    public void enablePost(Long id) {
        // 恢复帖子：把帖子 status 改成 1。
        Post post = getPost(id);
        post.setStatus(STATUS_APPROVED);
        postMapper.updateById(post);
        notifyContentReview(post.getUserId(), "你的帖子已恢复展示：" + post.getTitle(), "POST", post.getId());
    }

    @Override
    public void pinPost(Long id) {
        Post post = getApprovedPost(id);
        post.setIsPinned(1);
        post.setPinnedTime(LocalDateTime.now());
        postMapper.updateById(post);
    }

    @Override
    public void unpinPost(Long id) {
        Post post = getPost(id);
        post.setIsPinned(0);
        post.setPinnedTime(null);
        postMapper.updateById(post);
    }

    @Override
    public void featurePost(Long id) {
        Post post = getApprovedPost(id);
        post.setIsFeatured(1);
        postMapper.updateById(post);
    }

    @Override
    public void unfeaturePost(Long id) {
        Post post = getPost(id);
        post.setIsFeatured(0);
        postMapper.updateById(post);
    }

    @Override
    public void deleteComment(Long id) {
        // 隐藏评论：把评论 status 改成 0。
        PostComment comment = getComment(id);
        comment.setStatus(0);
        postCommentMapper.updateById(comment);
        notifyContentReview(comment.getUserId(), "你的评论已被隐藏", "COMMENT", comment.getId());
    }

    @Override
    public void restoreComment(Long id) {
        // 恢复评论：把评论 status 改成 1。
        PostComment comment = getComment(id);
        comment.setStatus(1);
        postCommentMapper.updateById(comment);
        notifyContentReview(comment.getUserId(), "你的评论已恢复展示", "COMMENT", comment.getId());
    }

    @Override
    public List<Post> listPosts(Integer status, String keyword) {
        // 后台帖子列表：可按状态和关键词筛选。
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getContentType, Post.CONTENT_TYPE_POST);
        wrapper.eq(Post::getStatus, status == null ? STATUS_PENDING : status);
        if (StringUtils.hasText(keyword)) {
            String word = keyword.trim();
            wrapper.and(item -> item.like(Post::getTitle, word)
                    .or()
                    .like(Post::getContent, word)
                    .or()
                    .like(Post::getTopic, word));
        }
        wrapper.orderByDesc(Post::getCreateTime);
        return postMapper.selectList(wrapper);
    }

    @Override
    public Post getPostDetail(Long id) {
        return getPost(id);
    }

    @Override
    public void reviewPost(Long id, StatusUpdateDTO dto) {
        // 审核帖子：状态 1 表示通过，状态 2 表示拒绝。
        if (dto == null || dto.getStatus() == null) {
            throw new RuntimeException("审核状态不能为空");
        }
        if (!Integer.valueOf(STATUS_APPROVED).equals(dto.getStatus()) && !Integer.valueOf(STATUS_HIDDEN).equals(dto.getStatus())) {
            throw new RuntimeException("审核状态只能是1或2");
        }

        Post post = getPost(id);
        post.setStatus(dto.getStatus());
        if (Integer.valueOf(STATUS_HIDDEN).equals(dto.getStatus())) {
            clearPostOperations(post);
        }
        postMapper.updateById(post);

        String action = Integer.valueOf(STATUS_APPROVED).equals(dto.getStatus()) ? "审核通过" : "审核未通过";
        String content = "你的帖子" + action + "：" + post.getTitle();
        if (StringUtils.hasText(dto.getRemark())) {
            content = content + "，备注：" + dto.getRemark();
        }
        notifyContentReview(post.getUserId(), content, "POST", post.getId());
    }

    @Override
    public List<Post> listVideos(Integer status, String keyword) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getContentType, Post.CONTENT_TYPE_VIDEO);
        wrapper.eq(Post::getStatus, status == null ? STATUS_PENDING : status);
        if (StringUtils.hasText(keyword)) {
            String word = keyword.trim();
            wrapper.and(item -> item.like(Post::getTitle, word)
                    .or()
                    .like(Post::getContent, word)
                    .or()
                    .like(Post::getTopic, word));
        }
        wrapper.orderByDesc(Post::getCreateTime);
        return postMapper.selectList(wrapper);
    }

    @Override
    public Post getVideoDetail(Long id) {
        return getVideo(id);
    }

    @Override
    public void reviewVideo(Long id, StatusUpdateDTO dto) {
        if (dto == null || dto.getStatus() == null) {
            throw new RuntimeException("短视频审核状态不能为空");
        }
        if (!Integer.valueOf(STATUS_APPROVED).equals(dto.getStatus())
                && !Integer.valueOf(STATUS_HIDDEN).equals(dto.getStatus())) {
            throw new RuntimeException("短视频审核状态只能是1或2");
        }

        Post video = getVideo(id);
        video.setStatus(dto.getStatus());
        if (Integer.valueOf(STATUS_HIDDEN).equals(dto.getStatus())) {
            clearPostOperations(video);
        }
        postMapper.updateById(video);

        String action = Integer.valueOf(STATUS_APPROVED).equals(dto.getStatus()) ? "审核通过" : "审核未通过";
        String content = "你的短视频" + action + "：" + video.getTitle();
        if (StringUtils.hasText(dto.getRemark())) {
            content = content + "，备注：" + dto.getRemark();
        }
        notifyContentReview(video.getUserId(), content, Post.CONTENT_TYPE_POST, video.getId());
    }

    @Override
    public void deleteVideo(Long id) {
        Post video = getVideo(id);
        video.setStatus(STATUS_BANNED);
        clearPostOperations(video);
        postMapper.updateById(video);
        notifyContentReview(video.getUserId(), "你的短视频已被删除：" + video.getTitle(), Post.CONTENT_TYPE_POST, video.getId());
    }

    @Override
    public void restoreVideo(Long id) {
        Post video = getVideo(id);
        video.setStatus(STATUS_APPROVED);
        postMapper.updateById(video);
        notifyContentReview(video.getUserId(), "你的短视频已恢复展示：" + video.getTitle(), Post.CONTENT_TYPE_POST, video.getId());
    }

    @Override
    public List<PostComment> listComments(Integer status, Long postId, String keyword) {
        // 后台评论列表：可按状态、帖子 ID 和关键词筛选。
        LambdaQueryWrapper<PostComment> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(PostComment::getStatus, status);
        }
        if (postId != null) {
            wrapper.eq(PostComment::getPostId, postId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(PostComment::getContent, keyword.trim());
        }
        wrapper.orderByDesc(PostComment::getCreateTime);
        return postCommentMapper.selectList(wrapper);
    }

    @Override
    public List<Topic> listAuditTopics(Integer status, String keyword) {
        // 后台话题审核列表：可按状态和关键词筛选。
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Topic::getStatus, status == null ? STATUS_PENDING : status);
        if (StringUtils.hasText(keyword)) {
            String word = keyword.trim();
            wrapper.and(item -> item.like(Topic::getName, word)
                    .or()
                    .like(Topic::getDescription, word));
        }
        wrapper.orderByDesc(Topic::getCreateTime);
        return topicMapper.selectList(wrapper);
    }

    @Override
    public void reviewTopic(Long id, TopicReviewDTO dto) {
        // 审核话题：通过后设置排序值，拒绝则记录为拒绝状态。
        if (dto == null || dto.getStatus() == null) {
            throw new RuntimeException("话题审核状态不能为空");
        }
        if (!Integer.valueOf(STATUS_APPROVED).equals(dto.getStatus()) && !Integer.valueOf(STATUS_HIDDEN).equals(dto.getStatus())) {
            throw new RuntimeException("话题审核状态只能是1或2");
        }
        if (Integer.valueOf(STATUS_APPROVED).equals(dto.getStatus()) && dto.getSort() == null) {
            throw new RuntimeException("通过话题时必须填写排序值");
        }

        Topic topic = getTopic(id);
        topic.setStatus(dto.getStatus());
        if (Integer.valueOf(STATUS_APPROVED).equals(dto.getStatus())) {
            topic.setSort(dto.getSort() == null ? 0 : dto.getSort());
        }
        topicMapper.updateById(topic);
    }

    @Override
    public void createTopic(TopicCreateDTO dto) {
        // 管理员创建话题：可以直接创建正式启用的话题。
        if (dto == null || !StringUtils.hasText(dto.getName())) {
            throw new RuntimeException("话题名称不能为空");
        }

        String name = dto.getName().trim();
        Topic existTopic = findTopicByName(name);
        if (existTopic != null) {
            if (Integer.valueOf(STATUS_HIDDEN).equals(existTopic.getStatus())) {
                existTopic.setDescription(dto.getDescription());
                existTopic.setCover(dto.getCover());
                existTopic.setSort(dto.getSort() == null ? 0 : dto.getSort());
                existTopic.setStatus(STATUS_APPROVED);
                topicMapper.updateById(existTopic);
                return;
            }
            throw new RuntimeException("话题已存在或正在审核");
        }

        Topic topic = new Topic();
        topic.setName(name);
        topic.setDescription(dto.getDescription());
        topic.setCover(dto.getCover());
        topic.setPostCount(0);
        topic.setSort(dto.getSort() == null ? 0 : dto.getSort());
        topic.setStatus(STATUS_APPROVED);
        topicMapper.insert(topic);
    }

    @Override
    public void deleteTopic(Long id) {
        // 删除话题：这里采用逻辑删除，把状态改成 0。
        Topic topic = getTopic(id);
        topic.setStatus(STATUS_HIDDEN);
        topicMapper.updateById(topic);
    }

    @Override
    public List<Appointment> listAppointments() {
        // 后台预约列表：查询全平台预约单。
        return appointmentMapper.selectList(
                new LambdaQueryWrapper<Appointment>()
                        .orderByDesc(Appointment::getCreateTime)
        );
    }

    @Override
    public List<ProductOrder> listProductOrders() {
        // 后台订单列表：查询全平台商城订单。
        return productOrderMapper.selectList(
                new LambdaQueryWrapper<ProductOrder>()
                        .orderByDesc(ProductOrder::getCreateTime)
        );
    }

    private User getUser(Long id) {
        // 公共用户查询：按 ID 获取用户，不存在就抛异常。
        if (id == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    private User getManageableUser(Long id, String adminMessage) {
        User user = getUser(id);
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException(adminMessage);
        }
        return user;
    }

    private void clearPassword(User user) {
        user.setPassword(null);
    }

    private void finishPendingMerchantApplications(Long userId,
                                                   Long operatorId,
                                                   Integer status,
                                                   String remark) {
        List<MerchantApplication> applications = merchantApplicationMapper.selectList(
                new LambdaQueryWrapper<MerchantApplication>()
                        .eq(MerchantApplication::getUserId, userId)
                        .eq(MerchantApplication::getStatus, STATUS_PENDING)
        );
        for (MerchantApplication application : applications) {
            application.setStatus(status);
            application.setReviewerId(operatorId);
            application.setReviewRemark(remark);
            application.setReviewTime(LocalDateTime.now());
            merchantApplicationMapper.updateById(application);
        }
    }

    private Post getPost(Long id) {
        // 公共帖子查询：按 ID 获取帖子，不存在就抛异常。
        if (id == null) {
            throw new RuntimeException("帖子ID不能为空");
        }
        Post post = postMapper.selectOne(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getId, id)
                        .eq(Post::getContentType, Post.CONTENT_TYPE_POST)
        );
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        return post;
    }

    private Post getApprovedPost(Long id) {
        Post post = getPost(id);
        if (!Integer.valueOf(STATUS_APPROVED).equals(post.getStatus())) {
            throw new RuntimeException("只有已审核帖子可以置顶或加精");
        }
        return post;
    }

    private Post getVideo(Long id) {
        if (id == null) {
            throw new RuntimeException("短视频ID不能为空");
        }
        Post video = postMapper.selectById(id);
        if (video == null || !Post.CONTENT_TYPE_VIDEO.equals(video.getContentType())) {
            throw new RuntimeException("短视频不存在");
        }
        return video;
    }

    private void clearPostOperations(Post post) {
        post.setIsPinned(0);
        post.setIsFeatured(0);
        post.setPinnedTime(null);
    }

    private PostComment getComment(Long id) {
        // 公共评论查询：按 ID 获取评论，不存在就抛异常。
        if (id == null) {
            throw new RuntimeException("评论ID不能为空");
        }
        PostComment comment = postCommentMapper.selectById(id);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        return comment;
    }

    private Topic getTopic(Long id) {
        // 公共话题查询：按 ID 获取话题，不存在就抛异常。
        if (id == null) {
            throw new RuntimeException("话题ID不能为空");
        }
        Topic topic = topicMapper.selectById(id);
        if (topic == null) {
            throw new RuntimeException("话题不存在");
        }
        return topic;
    }

    private Topic findTopicByName(String name) {
        // 按名称查话题，用于管理员创建话题时防重复。
        return topicMapper.selectOne(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getName, name)
        );
    }

    private void notifyContentReview(Long userId, String content, String targetType, Long targetId) {
        // 内容审核结果统一从这里通知用户。
        if (userId == null) {
            return;
        }
        notificationService.create(userId, null, "CONTENT_REVIEW", content, targetType, targetId);
    }
}
