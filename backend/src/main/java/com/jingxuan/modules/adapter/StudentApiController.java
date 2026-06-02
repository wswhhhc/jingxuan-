package com.jingxuan.modules.adapter;

import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.SysNotification;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.score.dto.MyRankVO;
import com.jingxuan.modules.work.dto.WorkCreateRequest;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.dto.WorkListVO;
import com.jingxuan.modules.work.dto.WorkQueryRequest;
import com.jingxuan.modules.work.dto.WorkUpdateRequest;
import com.jingxuan.modules.work.service.WorkService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生端 API 适配 — 桥接前端期望的学生端路径与后端实际接口
 */
@Tag(name = "学生端API适配")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentApiController {

    private final WorkService workService;
    private final StudentRankingFacade studentRankingFacade;
    private final NotificationService notificationService;

    @Operation(summary = "创建作品")
    @PostMapping("/student/works")
    public Result<Long> createWork(@Valid @RequestBody WorkCreateRequest request) {
        Long workId = workService.createWork(request);
        return Result.ok(workId);
    }

    @Operation(summary = "获取我的作品列表")
    @GetMapping("/student/works")
    public Result<PageResult<WorkListVO>> getMyWorks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        Long userId = SecurityUtils.requireCurrentUserId();
        WorkQueryRequest query = new WorkQueryRequest();
        query.setPageNum(page);
        query.setPageSize(size);
        query.setParticipantUserId(userId);
        query.setStatus(status);
        return Result.ok(workService.queryWorkList(query));
    }

    @Operation(summary = "获取作品详情")
    @GetMapping("/student/works/{id}")
    public Result<WorkDetailVO> getWorkDetail(@PathVariable Long id) {
        WorkDetailVO vo = workService.getCurrentStudentWorkDetail(id);
        return Result.ok(vo);
    }

    @Operation(summary = "更新作品")
    @PutMapping("/student/works/{id}")
    public Result<Void> updateWork(@PathVariable Long id,
                                   @Valid @RequestBody WorkUpdateRequest request) {
        workService.updateWork(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除作品")
    @DeleteMapping("/student/works/{id}")
    public Result<Void> deleteWork(@PathVariable Long id) {
        workService.deleteWork(id);
        return Result.ok();
    }

    @Operation(summary = "提交作品审核")
    @PostMapping("/student/works/{id}/submit")
    public Result<Void> submitWork(@PathVariable Long id) {
        workService.submitWork(id);
        return Result.ok();
    }

    @Operation(summary = "获取我的评分与排名（仅排行榜已公示时返回）")
    @GetMapping("/student/score/my-ranks")
    public Result<List<MyRankVO>> getMyRanks() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.ok(studentRankingFacade.getPublishedRanks(userId));
    }

    @Operation(summary = "获取学生通知列表")
    @GetMapping("/student/notify/list")
    public Result<PageResult<SysNotification>> listNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean unreadOnly) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.ok(notificationService.queryUserNotifications(userId, page, size, unreadOnly));
    }

    @Operation(summary = "标记学生通知已读")
    @PostMapping("/student/notify/read/{id}")
    public Result<Void> markNotificationRead(@PathVariable Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.ok();
    }

    @Operation(summary = "学生通知全部标记已读")
    @PostMapping("/student/notify/read-all")
    public Result<Void> markAllNotificationsRead() {
        Long userId = SecurityUtils.requireCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.ok();
    }

    @Operation(summary = "获取学生未读通知数")
    @GetMapping("/student/notify/unread-count")
    public Result<Map<String, Long>> getUnreadCount() {
        Long userId = SecurityUtils.requireCurrentUserId();
        long count = notificationService.countUnread(userId);
        Map<String, Long> map = new HashMap<>();
        map.put("count", count);
        return Result.ok(map);
    }
}
