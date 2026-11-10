package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.FollowActionDTO;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.entity.UserFollow;
import com.example.petcommunity3.mapper.UserFollowMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.NotificationService;
import com.example.petcommunity3.service.UserFollowService;
import com.example.petcommunity3.vo.FollowCountVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserFollowServiceImpl implements UserFollowService {
    // UserFollowServiceImpl 负责关注关系模块：关注、取关、关注列表、粉丝列表和关注状态。
    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public UserFollowServiceImpl(UserFollowMapper userFollowMapper,
                                 UserMapper userMapper,
                                 NotificationService notificationService) {
        this.userFollowMapper = userFollowMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Override
    public void follow(FollowActionDTO dto) {
        // 关注用户：校验双方用户，禁止关注自己和重复关注，然后写入 user_follow 表。
        if (dto.getFollowerId() == null) {
            throw new RuntimeException("关注者ID不能为空");
        }

        if (dto.getFollowedId() == null) {
            throw new RuntimeException("被关注者ID不能为空");
        }

        if (dto.getFollowerId().equals(dto.getFollowedId())) {
            throw new RuntimeException("不能关注自己");
        }

        User follower = getNormalUser(dto.getFollowerId(), "关注者不存在或已被禁用");
        getNormalUser(dto.getFollowedId(), "被关注者不存在或已被禁用");

        UserFollow existFollow = userFollowMapper.selectOne(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, dto.getFollowerId())
                        .eq(UserFollow::getFollowedId, dto.getFollowedId())
        );

        if (existFollow != null) {
            throw new RuntimeException("已经关注过该用户");
        }

        UserFollow userFollow = new UserFollow();
        userFollow.setFollowerId(dto.getFollowerId());
        userFollow.setFollowedId(dto.getFollowedId());
        userFollowMapper.insert(userFollow);

        notificationService.create(
                dto.getFollowedId(),
                dto.getFollowerId(),
                "FOLLOW",
                displayName(follower) + "关注了你",
                "USER",
                dto.getFollowerId()
        );
    }

    @Override
    public void unfollow(Long followerId, Long followedId) {
        // 取消关注：删除 followerId -> followedId 这一条关注关系。
        if (followerId == null) {
            throw new RuntimeException("关注者ID不能为空");
        }
        if (followedId == null) {
            throw new RuntimeException("被关注者ID不能为空");
        }

        userFollowMapper.delete(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, followerId)
                        .eq(UserFollow::getFollowedId, followedId)
        );
    }

    @Override
    public List<User> listFollowings(Long userId) {
        // 我关注的人：先查关注关系，再根据 followedId 查询用户信息。
        checkUserId(userId);

        List<UserFollow> follows = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId)
                        .orderByDesc(UserFollow::getCreateTime)
        );

        if (follows.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = follows.stream()
                .map(UserFollow::getFollowedId)
                .collect(Collectors.toList());

        List<User> users = userMapper.selectBatchIds(userIds);
        users.forEach(user -> user.setPassword(null));
        return users;
    }

    @Override
    public List<User> listFollowers(Long userId) {
        // 我的粉丝：先查关注关系，再根据 followerId 查询用户信息。
        checkUserId(userId);

        List<UserFollow> follows = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowedId, userId)
                        .orderByDesc(UserFollow::getCreateTime)
        );

        if (follows.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = follows.stream()
                .map(UserFollow::getFollowerId)
                .collect(Collectors.toList());

        List<User> users = userMapper.selectBatchIds(userIds);
        users.forEach(user -> user.setPassword(null));
        return users;
    }

    @Override
    public FollowCountVO count(Long userId) {
        // 关注统计：分别统计关注数和粉丝数。
        checkUserId(userId);

        FollowCountVO vo = new FollowCountVO();

        vo.setFollowingCount(userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId)
        ));

        vo.setFollowerCount(userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowedId, userId)
        ));

        return vo;
    }

    @Override
    public Boolean check(Long followerId, Long followedId) {
        // 查询是否已关注：用于前端决定按钮显示“关注”还是“已关注”。
        if (followerId == null || followedId == null) {
            return false;
        }

        Long count = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, followerId)
                        .eq(UserFollow::getFollowedId, followedId)
        );

        return count > 0;
    }

    private User getNormalUser(Long userId, String message) {
        // 公共用户校验：用户不存在或被禁用时抛异常。
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            throw new RuntimeException(message);
        }
        return user;
    }

    private void checkUserId(Long userId) {
        // 只校验用户 ID 是否为空，适合列表查询这种轻量操作。
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
    }

    private String displayName(User user) {
        // 通知文案里优先展示昵称，没有昵称再展示用户名。
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return user.getUsername();
    }
}
