package com.example.petcommunity3.service;

import com.example.petcommunity3.entity.UserNotification;

import java.util.List;

public interface NotificationService {
    // 创建通知：其他模块调用它，把关注、点赞、评论、预约、订单消息写入通知表。
    void create(Long userId, Long fromUserId, String type, String content, String targetType, Long targetId);

    // 查询用户通知列表：给消息中心页面使用。
    List<UserNotification> listByUserId(Long userId);

    // 查询未读数量：给前端显示消息红点。
    Long unreadCount(Long userId);

    // 标记单条已读：只能操作属于当前用户的通知。
    void markRead(Long id, Long userId);

    // 标记全部已读：把当前用户所有未读通知改为已读。
    void markAllRead(Long userId);
}
