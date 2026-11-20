package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.dto.BehaviorEventDTO;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.UserBehavior;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.UserBehaviorMapper;
import com.example.petcommunity3.service.BehaviorService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class BehaviorServiceImpl implements BehaviorService {
    private static final Set<String> VALID_ACTIONS = Set.of(
            UserBehavior.ACTION_IMPRESSION,
            UserBehavior.ACTION_PLAY,
            UserBehavior.ACTION_PLAY_END,
            UserBehavior.ACTION_LIKE,
            UserBehavior.ACTION_COMMENT,
            UserBehavior.ACTION_COLLECT,
            UserBehavior.ACTION_SHARE,
            UserBehavior.ACTION_SKIP
    );

    private final UserBehaviorMapper behaviorMapper;
    private final PostMapper postMapper;

    public BehaviorServiceImpl(UserBehaviorMapper behaviorMapper, PostMapper postMapper) {
        this.behaviorMapper = behaviorMapper;
        this.postMapper = postMapper;
    }

    @Override
    public void record(Long userId, BehaviorEventDTO dto) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        if (dto == null || dto.getTargetId() == null) {
            throw new RuntimeException("行为目标不能为空");
        }
        if (!StringUtils.hasText(dto.getAction()) || !VALID_ACTIONS.contains(dto.getAction())) {
            throw new RuntimeException("不支持的行为类型");
        }
        String targetType = StringUtils.hasText(dto.getTargetType()) ? dto.getTargetType() : "VIDEO";
        if (!"VIDEO".equals(targetType) && !"POST".equals(targetType)) {
            throw new RuntimeException("不支持的目标类型");
        }

        Post post = postMapper.selectById(dto.getTargetId());
        if (post == null || post.getStatus() == null || post.getStatus() != 1) {
            throw new RuntimeException("目标内容不存在或不可见");
        }
        if ("VIDEO".equals(targetType) && !Post.CONTENT_TYPE_VIDEO.equals(post.getContentType())) {
            throw new RuntimeException("目标不是短视频");
        }

        int watchMs = dto.getWatchMs() == null ? 0 : Math.max(0, dto.getWatchMs());
        double ratio = dto.getWatchRatio() == null ? 0d : Math.max(0d, Math.min(1d, dto.getWatchRatio()));

        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setTargetType(targetType);
        behavior.setTargetId(dto.getTargetId());
        behavior.setAction(dto.getAction());
        behavior.setWatchMs(watchMs);
        behavior.setWatchRatio(BigDecimal.valueOf(ratio).setScale(3, RoundingMode.HALF_UP));
        behavior.setScene(StringUtils.hasText(dto.getScene()) ? dto.getScene() : "FEED");
        behavior.setTraceId(StringUtils.hasText(dto.getTraceId()) ? dto.getTraceId() : null);
        behavior.setCreateTime(LocalDateTime.now());
        behaviorMapper.insert(behavior);
    }
}
