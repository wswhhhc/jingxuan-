package com.jingxuan.modules.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.RewardConfig;
import com.jingxuan.entity.RewardIssue;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysLog;
import com.jingxuan.entity.SysNotice;
import com.jingxuan.entity.SysNotification;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkPublish;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkAuditMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkPublishMapper;
import com.jingxuan.modules.prize.service.RewardIssueService;
import com.jingxuan.modules.audit.dto.AuditHistoryVO;
import com.jingxuan.modules.audit.dto.AuditRequest;
import com.jingxuan.modules.audit.service.AuditService;
import com.jingxuan.modules.comment.dto.AdminCommentVO;
import com.jingxuan.modules.comment.service.CommentService;
import com.jingxuan.modules.dict.service.DictService;
import com.jingxuan.modules.log.service.LogService;
import com.jingxuan.modules.notice.dto.NoticeRequest;
import com.jingxuan.modules.notice.service.NoticeService;
import com.jingxuan.modules.publish.dto.FeaturedRequest;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.prize.service.PrizeService;
import com.jingxuan.modules.publish.service.PublishService;
import com.jingxuan.modules.score.dto.AdminScoreDetailVO;
import com.jingxuan.modules.score.dto.ScoreVO;
import com.jingxuan.modules.score.service.ScoreService;
import com.jingxuan.modules.scorebatch.service.ScoreBatchService;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.dto.WorkListVO;
import com.jingxuan.modules.work.dto.WorkQueryRequest;
import com.jingxuan.modules.work.service.WorkService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "管理端 API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    private final WorkService workService;
    private final CommentService commentService;
    private final WorkPublishMapper workPublishMapper;
    private final WorkMapper workMapper;
    private final WorkAuditMapper workAuditMapper;
    private final SysUserMapper sysUserMapper;
    private final AuditService auditService;
    private final PublishService publishService;
    private final NoticeService noticeService;
    private final DictService dictService;
    private final LogService logService;
    private final ScoreBatchService scoreBatchService;
    private final PrizeService prizeService;
    private final RewardIssueService rewardIssueService;
    private final ScoreService scoreService;
    private final NotificationService notificationService;

    // ==================== Dashboard ====================

    @GetMapping("/admin/dashboard/stats")
    @Operation(summary = "获取仪表盘统计数据")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> map = new HashMap<>();

        // 作品总数
        Long totalWorks = workMapper.selectCount(null);
        map.put("totalWorks", totalWorks);

        // 待审核（status = 1）
        Long pendingAudit = workMapper.selectCount(
                new LambdaQueryWrapper<Work>().eq(Work::getStatus, 1));
        map.put("pendingAudit", pendingAudit);

        // 已发布作品
        Long publishedWorks = workPublishMapper.selectCount(
                new LambdaQueryWrapper<WorkPublish>().eq(WorkPublish::getPublishStatus, 1));
        map.put("publishedWorks", publishedWorks);

        // 教师总数（role_id = 2）
        Long totalTeachers = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, 2));
        map.put("totalTeachers", totalTeachers);

        // 学生总数（role_id = 1）
        Long totalStudents = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, 1));
        map.put("totalStudents", totalStudents);

        // 进行中的批次
        List<ScoreBatch> batches = scoreBatchService.list();
        long activeBatches = batches.stream().filter(b -> b.getStatus() != null && b.getStatus() == 1).count();
        map.put("activeBatches", activeBatches);

        // 最近提交的 5 个作品（补全 submitterName）
        Page<Work> recentPage = new Page<>(1, 5);
        LambdaQueryWrapper<Work> recentWrapper = new LambdaQueryWrapper<Work>()
                .orderByDesc(Work::getSubmitTime);
        workMapper.selectPage(recentPage, recentWrapper);
        List<Map<String, Object>> recentWorks = recentPage.getRecords().stream().map(w -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", w.getId());
            item.put("title", w.getTitle());
            item.put("techStack", w.getTechStack());
            item.put("status", w.getStatus());
            item.put("submitTime", w.getSubmitTime());
            SysUser user = sysUserMapper.selectById(w.getSubmitterId());
            item.put("submitterName", user != null ? user.getRealName() : null);
            return item;
        }).collect(java.util.stream.Collectors.toList());
        map.put("recentWorks", recentWorks);

        // 评分分布（占位）
        map.put("scoreDistribution", List.of());

        return Result.ok(map);
    }

    // ==================== Audit ====================

    @GetMapping("/admin/comment/list")
    @Operation(summary = "获取评论列表（管理端）")
    public Result<PageResult<AdminCommentVO>> listComments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long workId,
            @RequestParam(required = false) String userKeyword,
            @RequestParam(required = false) String contentKeyword) {
        return Result.ok(commentService.getAdminComments(page, size, workId, userKeyword, contentKeyword));
    }

    @GetMapping("/admin/comment/work-options")
    @Operation(summary = "获取评论涉及的作品选项")
    public Result<List<Map<String, Object>>> listCommentWorkOptions() {
        List<Long> workIds = commentService.list().stream()
                .map(com.jingxuan.entity.WorkComment::getWorkId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        if (workIds.isEmpty()) {
            return Result.ok(List.of());
        }
        List<Map<String, Object>> records = workService.listByIds(workIds).stream()
                .map(work -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("workId", work.getId());
                    item.put("workTitle", work.getTitle());
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());
        return Result.ok(records);
    }

    @DeleteMapping("/admin/comment/{commentId}")
    @Operation(summary = "删除评论（管理端）")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(
                commentId,
                SecurityUtils.requireCurrentUserId(),
                SecurityUtils.getCurrentRoleCode());
        return Result.ok();
    }

    @GetMapping("/admin/audit/list")
    @Operation(summary = "获取审核列表（管理端）")
    public Result<PageResult<WorkListVO>> listAuditWorks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String submitTimeBegin,
            @RequestParam(required = false) String submitTimeEnd) {
        WorkQueryRequest request = new WorkQueryRequest();
        request.setPageNum(page);
        request.setPageSize(size);
        request.setStatus(status);
        request.setKeyword(keyword);
        request.setClassId(classId);
        request.setSubmitTimeBegin(submitTimeBegin);
        request.setSubmitTimeEnd(submitTimeEnd);
        return Result.ok(workService.queryWorkList(request));
    }

    @GetMapping("/admin/audit/{workId}")
    @Operation(summary = "获取作品详情（审核用）")
    public Result<WorkDetailVO> getAuditWorkDetail(@PathVariable Long workId) {
        return Result.ok(workService.getWorkDetail(workId));
    }

    @PostMapping("/admin/audit")
    @Operation(summary = "提交审核结果")
    public Result<Void> submitAudit(@RequestBody Map<String, Object> body) {
        Long workId = Long.valueOf(body.get("workId").toString());
        String result = (String) body.get("result");
        String reason = (String) body.get("reason");

        AuditRequest request = new AuditRequest();
        request.setWorkId(workId);
        request.setReason(reason);

        if ("approved".equals(result)) {
            request.setResult(1);
            auditService.approve(request);
        } else {
            request.setResult(0);
            auditService.reject(request);
        }
        return Result.ok();
    }

    @GetMapping("/admin/audit/{workId}/history")
    @Operation(summary = "查询审核记录")
    public Result<PageResult<AuditHistoryVO>> getAuditHistory(
            @PathVariable Long workId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(auditService.queryHistory(workId, page, size));
    }

    @PostMapping("/admin/audit/{workId}/publish")
    @Operation(summary = "发布作品")
    public Result<Void> publishWork(@PathVariable Long workId) {
        publishService.publishWork(workId);
        return Result.ok();
    }

    @PostMapping("/admin/audit/{workId}/offline")
    @Operation(summary = "下线作品")
    public Result<Void> offlineWork(@PathVariable Long workId) {
        publishService.offlineWork(workId);
        return Result.ok();
    }

    @PostMapping("/admin/audit/{workId}/featured")
    @Operation(summary = "设置/取消精选作品并配置预览地址")
    public Result<Void> setFeatured(@PathVariable Long workId,
                                    @RequestParam Integer featured,
                                    @RequestParam(required = false) String previewUrl) {
        FeaturedRequest request = new FeaturedRequest();
        request.setWorkId(workId);
        request.setFeatured(featured);
        request.setPreviewUrl(previewUrl);
        publishService.setFeatured(request);
        return Result.ok();
    }

    // ==================== Notice ====================

    @GetMapping("/admin/notice/list")
    @Operation(summary = "获取公告列表")
    public Result<PageResult<SysNotice>> listNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(noticeService.queryNoticeList(page, size, status));
    }

    @GetMapping("/admin/notice/{id}")
    @Operation(summary = "获取公告详情")
    public Result<SysNotice> getNotice(@PathVariable Long id) {
        return Result.ok(noticeService.getById(id));
    }

    @PostMapping("/admin/notice")
    @Operation(summary = "创建公告")
    public Result<Long> createNotice(@Valid @RequestBody NoticeRequest request) {
        Long publisherId = SecurityUtils.requireCurrentUserId();
        Long id = noticeService.createNotice(request, publisherId);
        return Result.ok(id);
    }

    @PutMapping("/admin/notice/{id}")
    @Operation(summary = "更新公告")
    public Result<Void> updateNotice(@PathVariable Long id, @Valid @RequestBody NoticeRequest request) {
        noticeService.updateNotice(id, request);
        return Result.ok();
    }

    @DeleteMapping("/admin/notice/{id}")
    @Operation(summary = "删除公告")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        noticeService.removeById(id);
        return Result.ok();
    }

    // ==================== Dict ====================

    @GetMapping("/admin/dict/type/{dictType}")
    @Operation(summary = "根据字典类型获取字典数据")
    public Result<List<SysDict>> getDictByType(@PathVariable String dictType) {
        return Result.ok(dictService.getByType(dictType));
    }

    @GetMapping("/admin/dict/all")
    @Operation(summary = "获取所有字典（按类型分组）")
    public Result<Map<String, List<SysDict>>> getAllDicts() {
        return Result.ok(dictService.getAllGroupByType());
    }

    @GetMapping("/admin/dict/classes")
    @Operation(summary = "获取班级字典列表")
    public Result<List<SysDict>> getClasses() {
        return Result.ok(dictService.getByType("class"));
    }

    @GetMapping("/admin/dict/list")
    @Operation(summary = "获取字典列表（按类型）")
    public Result<List<SysDict>> getDictList(@RequestParam String type) {
        return Result.ok(dictService.getByType(type));
    }

    @PostMapping("/admin/dict/create")
    @Operation(summary = "创建字典项")
    public Result<Long> createDict(@Valid @RequestBody SysDict dict) {
        return Result.ok(dictService.createDict(dict));
    }

    @PutMapping("/admin/dict/update")
    @Operation(summary = "更新字典项")
    public Result<Void> updateDict(@Valid @RequestBody SysDict dict) {
        dictService.updateDict(dict);
        return Result.ok();
    }

    @DeleteMapping("/admin/dict/{id}")
    @Operation(summary = "删除字典项")
    public Result<Void> deleteDict(@PathVariable Long id) {
        dictService.removeById(id);
        return Result.ok();
    }

    // ==================== Log ====================

    @GetMapping("/admin/log/list")
    @Operation(summary = "获取操作日志列表")
    public Result<PageResult<SysLog>> listLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId) {
        return Result.ok(logService.queryLogList(page, size, action, userId));
    }

    // ==================== Notification ====================

    @GetMapping("/admin/notify/unread-count")
    @Operation(summary = "获取未读通知数（管理端）")
    public Result<Map<String, Long>> getUnreadCount() {
        Long userId = SecurityUtils.requireCurrentUserId();
        long count = notificationService.countUnread(userId);
        Map<String, Long> map = new HashMap<>();
        map.put("count", count);
        return Result.ok(map);
    }

    @GetMapping("/admin/notify/list")
    @Operation(summary = "获取通知列表（管理端）")
    public Result<PageResult<SysNotification>> listNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.ok(notificationService.queryUserNotifications(userId, page, size, null));
    }

    @PostMapping("/admin/notify/read/{id}")
    @Operation(summary = "标记通知已读（管理端）")
    public Result<Void> markNotificationRead(@PathVariable Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.ok();
    }

    @PostMapping("/admin/notify/read-all")
    @Operation(summary = "全部标记已读（管理端）")
    public Result<Void> markAllNotificationsRead() {
        Long userId = SecurityUtils.requireCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.ok();
    }

    // ==================== Score Batch ====================

    @GetMapping("/admin/score-batch/list")
    @Operation(summary = "获取评分批次列表")
    public Result<PageResult<ScoreBatch>> listScoreBatches(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(scoreBatchService.queryBatchList(page, size));
    }

    // ==================== Prize ====================

    @GetMapping("/admin/prize/list")
    @Operation(summary = "获取奖品列表")
    public Result<PageResult<com.jingxuan.modules.prize.dto.PrizeVO>> listPrizes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long batchId) {
        return Result.ok(prizeService.queryPrizeList(page, size, batchId));
    }

    @PostMapping("/admin/prize")
    @Operation(summary = "新增奖品")
    public Result<Long> createPrize(@RequestBody RewardConfig config) {
        return Result.ok(prizeService.createPrize(config));
    }

    @PutMapping("/admin/prize/{id}")
    @Operation(summary = "更新奖品")
    public Result<Void> updatePrize(@PathVariable Long id, @RequestBody RewardConfig config) {
        config.setId(id);
        prizeService.updatePrize(config);
        return Result.ok();
    }

    @DeleteMapping("/admin/prize/{id}")
    @Operation(summary = "删除奖品")
    public Result<Void> deletePrize(@PathVariable Long id) {
        prizeService.deletePrize(id);
        return Result.ok();
    }

    @GetMapping("/admin/prize/batches")
    @Operation(summary = "获取评分批次列表（奖品筛选用）")
    public Result<List<ScoreBatch>> listPrizeBatches() {
        return Result.ok(scoreBatchService.list());
    }

    // ==================== Reward Issue (发放追踪) ====================

    @GetMapping("/admin/prize/issue/list")
    @Operation(summary = "获取奖品发放记录列表")
    public Result<PageResult<RewardIssue>> listIssueRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long rewardId) {
        Page<RewardIssue> mpPage = rewardIssueService.listByPage(page, size, rewardId);
        return Result.ok(PageResult.of(mpPage.getRecords(), mpPage.getTotal(), page, size));
    }

    @PostMapping("/admin/prize/issue")
    @Operation(summary = "发放奖品")
    public Result<Void> issuePrize(@RequestBody Map<String, Long> body) {
        Long rewardId = body.get("rewardId");
        Long workId = body.get("workId");
        Long operatorId = body.get("operatorId");
        rewardIssueService.issue(rewardId, workId, operatorId);
        return Result.ok();
    }

    @PutMapping("/admin/prize/issue/{id}/cancel")
    @Operation(summary = "取消奖品发放")
    public Result<Void> cancelIssue(@PathVariable Long id) {
        rewardIssueService.cancelIssue(id);
        return Result.ok();
    }

    // ==================== Score Detail ====================

    @GetMapping("/admin/score/batch/{batchId}")
    @Operation(summary = "获取评分批次评分明细（含各教师评分）")
    public Result<List<AdminScoreDetailVO>> getBatchScoreDetail(@PathVariable Long batchId) {
        // 查询该批次下所有已通过的作品
        List<Work> works = workMapper.selectList(
                new LambdaQueryWrapper<Work>()
                        .eq(Work::getBatchId, batchId)
                        .eq(Work::getStatus, 3));
        List<AdminScoreDetailVO> result = new ArrayList<>();
        for (Work work : works) {
            AdminScoreDetailVO detail = new AdminScoreDetailVO();
            detail.setWorkId(work.getId());
            detail.setWorkTitle(work.getTitle());
            SysUser user = sysUserMapper.selectById(work.getSubmitterId());
            detail.setSubmitterName(user != null ? user.getRealName() : null);

            // 各教师评分
            List<ScoreVO> scores = scoreService.getWorkScores(work.getId());
            List<AdminScoreDetailVO.TeacherScoreItem> items = new ArrayList<>();
            for (ScoreVO s : scores) {
                AdminScoreDetailVO.TeacherScoreItem item = new AdminScoreDetailVO.TeacherScoreItem();
                item.setTeacherName(s.getTeacherName());
                item.setInnovation(s.getInnovation());
                item.setDifficulty(s.getDifficulty());
                item.setCompletion(s.getCompletion());
                item.setPracticality(s.getPracticality());
                item.setTotal(s.getTotal());
                item.setComment(s.getComment());
                items.add(item);
            }
            detail.setScores(items);
            result.add(detail);
        }
        return Result.ok(result);
    }

    // ==================== Chart Data ====================

    @GetMapping("/admin/dashboard/charts")
    @Operation(summary = "获取仪表盘图表数据")
    public Result<Map<String, Object>> getChartData() {
        Map<String, Object> data = new HashMap<>();

        // 作品按技术栈分布
        Map<String, Long> techCount = new HashMap<>();
        for (Work w : workMapper.selectList(null)) {
            if (w.getTechStack() != null && !w.getTechStack().isEmpty()) {
                for (String tech : w.getTechStack().split(",")) {
                    String t = tech.trim();
                    if (!t.isEmpty()) {
                        techCount.merge(t, 1L, Long::sum);
                    }
                }
            }
        }
        List<Map<String, Object>> techStackDist = new ArrayList<>();
        for (Map.Entry<String, Long> e : techCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", e.getKey());
            item.put("value", e.getValue());
            techStackDist.add(item);
        }
        data.put("techStackDist", techStackDist);

        // 作品按审核状态分布
        Map<String, Long> statusDist = new LinkedHashMap<>();
        statusDist.put("草稿", 0L);
        statusDist.put("已提交", 0L);
        statusDist.put("已驳回", 0L);
        statusDist.put("已通过", 0L);
        statusDist.put("其他", 0L);
        for (Work w : workMapper.selectList(null)) {
            Integer s = w.getStatus();
            String key;
            if (s == null) key = "其他";
            else switch (s) {
                case 0: key = "草稿"; break;
                case 1: key = "已提交"; break;
                case 2: key = "已驳回"; break;
                case 3: key = "已通过"; break;
                default: key = "其他"; break;
            }
            statusDist.merge(key, 1L, Long::sum);
        }
        data.put("statusDist", statusDist);

        // 评分分布
        try {
            data.put("scoreDist", workMapper.selectScoreDistribution());
        } catch (Exception e) {
            data.put("scoreDist", List.of());
        }

        return Result.ok(data);
    }
}
