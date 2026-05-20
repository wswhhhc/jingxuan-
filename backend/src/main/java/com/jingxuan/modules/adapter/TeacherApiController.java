package com.jingxuan.modules.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysNotification;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkScore;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkScoreMapper;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.rank.dto.RankQueryRequest;
import com.jingxuan.modules.rank.dto.RankVO;
import com.jingxuan.modules.rank.service.RankService;
import com.jingxuan.modules.score.dto.ScoreSubmitRequest;
import com.jingxuan.modules.score.dto.ScoreVO;
import com.jingxuan.modules.score.dto.TeacherScoreHistoryVO;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "教师端 API")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherApiController {

    private final WorkService workService;
    private final ScoreService scoreService;
    private final ScoreBatchService scoreBatchService;
    private final RankService rankService;
    private final NotificationService notificationService;
    private final WorkScoreMapper workScoreMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDictMapper sysDictMapper;

    // ==================== Works ====================

    @GetMapping("/teacher/work/list")
    @Operation(summary = "查询作品列表（教师端，仅展示已审核通过的作品）")
    public Result<PageResult<WorkListVO>> listWorks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String techStack,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Boolean onlyUnscored) {
        WorkQueryRequest request = new WorkQueryRequest();
        request.setPageNum(page);
        request.setPageSize(size);
        request.setKeyword(keyword);
        request.setTechStack(techStack);
        request.setBatchId(batchId);
        request.setStatus(3); // APPROVED

        // 标记当前教师已评分的作品
        Long teacherId = SecurityUtils.requireCurrentUserId();
        Set<Long> scoredIds = workScoreMapper.selectList(
                new LambdaQueryWrapper<WorkScore>()
                        .eq(WorkScore::getTeacherId, teacherId))
                .stream().map(WorkScore::getWorkId).collect(Collectors.toSet());
        if (Boolean.TRUE.equals(onlyUnscored) && !scoredIds.isEmpty()) {
            request.setExcludeWorkIds(scoredIds.stream().collect(Collectors.toList()));
        }
        PageResult<WorkListVO> result = workService.queryWorkList(request);
        result.getRecords().forEach(vo -> vo.setScored(scoredIds.contains(vo.getId())));

        return Result.ok(result);
    }

    @GetMapping("/teacher/work/{id}")
    @Operation(summary = "获取作品详情（匿名评审，隐藏学生姓名和成员信息）")
    public Result<WorkDetailVO> getWorkDetail(@PathVariable Long id) {
        WorkDetailVO vo = workService.getApprovedWorkDetail(id);
        vo.setSubmitterName(null);
        vo.setSubmitterId(null);
        vo.setMembers(null);
        return Result.ok(vo);
    }

    // ==================== Score ====================

    @PostMapping("/teacher/score")
    @Operation(summary = "提交评分")
    public Result<Void> submitScore(@Valid @RequestBody ScoreSubmitRequest request) {
        Long teacherId = SecurityUtils.requireCurrentUserId();
        scoreService.submitScore(teacherId, request);
        return Result.ok();
    }

    @GetMapping("/teacher/score/{workId}")
    @Operation(summary = "获取教师对某作品的评分")
    public Result<ScoreVO> getTeacherScore(@PathVariable Long workId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.ok(scoreService.getTeacherScore(workId, userId));
    }

    @GetMapping("/teacher/score/history")
    @Operation(summary = "获取评分历史")
    public Result<PageResult<TeacherScoreHistoryVO>> getScoreHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long teacherId = SecurityUtils.requireCurrentUserId();
        Page<WorkScore> scorePage = new Page<>(page, size);
        LambdaQueryWrapper<WorkScore> wrapper = new LambdaQueryWrapper<WorkScore>()
                .eq(WorkScore::getTeacherId, teacherId)
                .orderByDesc(WorkScore::getUpdateTime)
                .orderByDesc(WorkScore::getId);
        workScoreMapper.selectPage(scorePage, wrapper);
        List<WorkScore> records = scorePage.getRecords();
        Map<Long, String> workTitleMap = records.isEmpty()
                ? Collections.emptyMap()
                : workService.listByIds(records.stream()
                                .map(WorkScore::getWorkId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .collect(Collectors.toList()))
                        .stream()
                        .collect(Collectors.toMap(Work::getId, Work::getTitle));

        List<TeacherScoreHistoryVO> voList = records.stream().map(score -> {
            TeacherScoreHistoryVO vo = new TeacherScoreHistoryVO();
            vo.setId(score.getId());
            vo.setWorkId(score.getWorkId());
            vo.setWorkTitle(workTitleMap.getOrDefault(score.getWorkId(), "作品#" + score.getWorkId()));
            vo.setBatchId(score.getBatchId());
            vo.setInnovation(score.getInnovation());
            vo.setDifficulty(score.getDifficulty());
            vo.setCompletion(score.getCompletion());
            vo.setPracticality(score.getPracticality());
            vo.setTotal(score.getTotal());
            vo.setComment(score.getComment());
            vo.setScoreTime(score.getUpdateTime() != null ? score.getUpdateTime() : score.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        PageResult<TeacherScoreHistoryVO> pageResult = new PageResult<>(
                voList, scorePage.getTotal(), scorePage.getCurrent(), scorePage.getSize());
        return Result.ok(pageResult);
    }

    @GetMapping("/teacher/dashboard/stats")
    @Operation(summary = "获取教师工作台统计")
    public Result<Map<String, Object>> getDashboardStats() {
        Long teacherId = SecurityUtils.requireCurrentUserId();
        List<Work> approvedWorks = workService.list(
                new LambdaQueryWrapper<Work>()
                        .eq(Work::getStatus, 3));

        Set<Long> scorableWorkIds = approvedWorks.stream()
                .filter(this::isWorkScorableForTeacher)
                .map(Work::getId)
                .collect(Collectors.toCollection(HashSet::new));

        Set<Long> scoredWorkIds = workScoreMapper.selectList(
                        new LambdaQueryWrapper<WorkScore>()
                                .eq(WorkScore::getTeacherId, teacherId))
                .stream()
                .map(WorkScore::getWorkId)
                .filter(scorableWorkIds::contains)
                .collect(Collectors.toSet());

        int totalScorableWorks = scorableWorkIds.size();
        int scoredWorks = scoredWorkIds.size();
        int pendingWorks = Math.max(totalScorableWorks - scoredWorks, 0);
        long activeBatchCount = scoreBatchService.list().stream()
                .filter(batch -> batch.getStatus() != null && batch.getStatus() == 1)
                .count();
        long unreadCount = notificationService.countUnread(teacherId);

        Map<String, Object> data = new HashMap<>();
        data.put("pendingWorks", pendingWorks);
        data.put("scoredWorks", scoredWorks);
        data.put("totalScorableWorks", totalScorableWorks);
        data.put("completionRate", totalScorableWorks == 0 ? 0 : Math.round(scoredWorks * 100.0 / totalScorableWorks));
        data.put("activeBatchCount", activeBatchCount);
        data.put("unreadCount", unreadCount);
        return Result.ok(data);
    }

    // ==================== Batch ====================

    @GetMapping("/teacher/batch/list")
    @Operation(summary = "获取评分批次列表")
    public Result<List<ScoreBatch>> listBatches() {
        return Result.ok(scoreBatchService.list());
    }

    // ==================== Ranking ====================

    @GetMapping("/teacher/ranking/list")
    @Operation(summary = "获取排行榜")
    public Result<List<RankVO>> listRanking(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false, defaultValue = "10") int topN,
            @RequestParam(required = false) String type) {
        RankQueryRequest request = new RankQueryRequest();
        request.setBatchId(batchId);
        request.setTopN(topN);
        request.setTechStack(type);
        return Result.ok(rankService.getRankList(request));
    }

    @GetMapping("/teacher/ranking/batches")
    @Operation(summary = "获取排行榜批次列表")
    public Result<List<ScoreBatch>> listRankingBatches() {
        return Result.ok(scoreBatchService.list());
    }

    @GetMapping("/teacher/ranking/categories")
    @Operation(summary = "获取排行分类（技术栈）")
    public Result<List<Map<String, String>>> listRankingCategories() {
        List<Map<String, String>> categories = workService.list().stream()
                .map(Work::getTechStack)
                .filter(Objects::nonNull)
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .map(tech -> {
                    Map<String, String> item = new HashMap<>();
                    item.put("label", tech);
                    item.put("value", tech);
                    return item;
                })
                .collect(Collectors.toList());
        return Result.ok(categories);
    }

    @PostMapping("/teacher/ranking/refresh")
    @Operation(summary = "刷新排行榜缓存")
    public Result<Void> refreshRanking(@RequestParam(required = false) Long batchId) {
        rankService.refreshRankCache(batchId);
        return Result.ok();
    }

    @PostMapping("/teacher/ranking/refresh/{batchId}")
    @Operation(summary = "刷新排行榜缓存（按批次）")
    public Result<Void> refreshRankingByBatch(@PathVariable Long batchId) {
        rankService.refreshRankCache(batchId);
        return Result.ok();
    }

    // ==================== Notification ====================

    @GetMapping("/teacher/notify/list")
    @Operation(summary = "获取通知列表")
    public Result<PageResult<SysNotification>> listNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean unreadOnly) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.ok(notificationService.queryUserNotifications(userId, page, size, unreadOnly));
    }

    @PostMapping("/teacher/notify/read/{id}")
    @Operation(summary = "标记通知已读")
    public Result<Void> markNotificationRead(@PathVariable Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.ok();
    }

    @PostMapping("/teacher/notify/read-all")
    @Operation(summary = "全部标记已读")
    public Result<Void> markAllNotificationsRead() {
        Long userId = SecurityUtils.requireCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.ok();
    }

    @GetMapping("/teacher/notify/unread-count")
    @Operation(summary = "获取未读通知数")
    public Result<Map<String, Long>> getUnreadCount() {
        Long userId = SecurityUtils.requireCurrentUserId();
        long count = notificationService.countUnread(userId);
        Map<String, Long> map = new HashMap<>();
        map.put("count", count);
        return Result.ok(map);
    }

    private boolean isWorkScorableForTeacher(Work work) {
        if (work.getBatchId() == null) {
            return true;
        }
        ScoreBatch batch = scoreBatchService.getById(work.getBatchId());
        if (batch == null) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        if (batch.getStartTime() != null && now.isBefore(batch.getStartTime())) {
            return false;
        }
        if (batch.getEndTime() != null && now.isAfter(batch.getEndTime())) {
            return false;
        }
        if (batch.getClassScopes() == null || batch.getClassScopes().isBlank()) {
            return true;
        }
        SysUser submitter = sysUserMapper.selectById(work.getSubmitterId());
        if (submitter == null || submitter.getClassId() == null) {
            return true;
        }

        Set<String> scopes = Arrays.stream(batch.getClassScopes()
                        .replace("[", "")
                        .replace("]", "")
                        .replace("\"", "")
                        .split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toSet());
        String classId = String.valueOf(submitter.getClassId());
        SysDict classDict = sysDictMapper.selectById(submitter.getClassId());
        String classValue = classDict != null ? classDict.getDictValue() : null;
        return scopes.contains(classId) || (classValue != null && !classValue.isBlank() && scopes.contains(classValue));
    }
}
