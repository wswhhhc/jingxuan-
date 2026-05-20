package com.jingxuan.modules.notification.controller;

import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.SysNotification;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 消息通知接口
 */
@Tag(name = "消息通知")
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "查询用户通知列表")
    @GetMapping("/list")
    public Result<PageResult<SysNotification>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = SecurityUtils.requireCurrentUserId();
        PageResult<SysNotification> pageResult = notificationService.queryUserNotifications(userId, pageNum, pageSize, null);
        return Result.ok(pageResult);
    }

    @Operation(summary = "标记通知已读")
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.ok();
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public Result<Void> readAll() {
        Long userId = SecurityUtils.requireCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.ok();
    }

    @Operation(summary = "获取未读通知数")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = SecurityUtils.requireCurrentUserId();
        long count = notificationService.countUnread(userId);
        return Result.ok(count);
    }
}
