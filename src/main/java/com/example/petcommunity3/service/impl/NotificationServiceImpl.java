package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.entity.UserNotification;
import com.example.petcommunity3.mapper.UserNotificationMapper;
import com.example.petcommunity3.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {
    // NotificationServiceImpl 负责消息通知模块：创建通知、查询通知、标记已读。
    private final UserNotificationMapper userNotificationMapper;

    public NotificationServiceImpl(UserNotificationMapper userNotificationMapper) {
        this.userNotificationMapper = userNotificationMapper;
    }

    @Override
    public void create(Long userId, Long fromUserId, String type, String content, String targetType, Long targetId) {
        // 创建通知：其它模块发生互动、预约、订单状态变化时会调用这里。
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setFromUserId(fromUserId);
        notification.setType(type);
        notification.setContent(content);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setReadStatus(0);

        userNotificationMapper.insert(notification);
    }

    @Override
    public List<UserNotification> listByUserId(Long userId) {
        // 查询通知列表：按用户 ID 查询，并按创建时间倒序展示。
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        return userNotificationMapper.selectList(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, userId)
                        .orderByDesc(UserNotification::getCreateTime)
        );
    }

    @Override
    public Long unreadCount(Long userId) {
        // 未读数量：统计当前用户 readStatus=0 的通知。
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        return userNotificationMapper.selectCount(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, userId)
                        .eq(UserNotification::getReadStatus, 0)
        );
    }

    @Override
    public void markRead(Long id, Long userId) {
        // 标记单条已读：用通知 ID + 用户 ID 保证只能改自己的通知。
        UserNotification notification = userNotificationMapper.selectById(id);

        if (notification == null) {
            throw new RuntimeException("通知不存在");
        }

        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("不能操作别人的通知");
        }

        notification.setReadStatus(1);
        userNotificationMapper.updateById(notification);
    }

    @Override
    public void markAllRead(Long userId) {
        // 全部已读：把当前用户所有未读通知统一改成已读。
        List<UserNotification> notifications = userNotificationMapper.selectList(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, userId)
                        .eq(UserNotification::getReadStatus, 0)
        );

        for (UserNotification notification : notifications) {
            notification.setReadStatus(1);
            userNotificationMapper.updateById(notification);
        }
    }
}