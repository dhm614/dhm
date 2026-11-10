package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.entity.UserNotification;
import com.example.petcommunity3.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    // 通知接口入口：查看消息、统计未读数、标记已读。
    private final NotificationService notificationService;

    // 注入通知业务层，Controller 不关心通知来自关注、评论、预约还是订单。
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // GET /api/notifications?userId=1
    // 查询某个用户收到的全部通知，按创建时间倒序返回。
    @GetMapping
    public Result<List<UserNotification>> list(@RequestParam Long userId) {
        return Result.success(notificationService.listByUserId(userId));
    }

    // GET /api/notifications/unread-count?userId=1
    // 查询未读通知数量，前端可以用来显示消息红点。
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestParam Long userId) {
        return Result.success(notificationService.unreadCount(userId));
    }

    // PUT /api/notifications/{id}/read?userId=1
    // 把单条通知标记为已读，同时校验通知是否属于当前用户。
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, @RequestParam Long userId) {
        notificationService.markRead(id, userId);
        return Result.success(null);
    }

    // PUT /api/notifications/read-all?userId=1
    // 把某个用户的所有未读通知一次性标记为已读。
    @PutMapping("/read-all")
    public Result<Void> markAllRead(@RequestParam Long userId) {
        notificationService.markAllRead(userId);
        return Result.success(null);
    }
}
