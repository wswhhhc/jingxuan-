package com.jingxuan.modules.score.controller;

import com.jingxuan.common.Result;
import com.jingxuan.entity.Work;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.score.dto.ScoreSubmitRequest;
import com.jingxuan.modules.score.service.ScoreService;
import com.jingxuan.modules.scorebatch.service.ScoreBatchService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评分管理接口
 */
@Tag(name = "评分管理", description = "作品评分、查看评分与汇总")
@RestController
@RequestMapping("/api/score")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;
    private final WorkMapper workMapper;
    private final ScoreBatchService scoreBatchService;

    @Operation(summary = "提交评分")
    @PostMapping("/submit")
    public Result<Void> submit(@Valid @RequestBody ScoreSubmitRequest request) {
        Long teacherId = SecurityUtils.requireCurrentUserId();
        scoreService.submitScore(teacherId, request);
        return Result.ok();
    }

    @Operation(summary = "获取我对作品的评分")
    @GetMapping("/my/{workId}")
    public Result<Object> getMyScore(@PathVariable Long workId) {
        Long teacherId = SecurityUtils.requireCurrentUserId();
        Object scoreVO = scoreService.getTeacherScore(workId, teacherId);
        return Result.ok(scoreVO);
    }

    @Operation(summary = "获取作品的所有评分（仅排行榜已公示时可见）")
    @GetMapping("/work/{workId}")
    public Result<Object> getWorkScores(@PathVariable Long workId) {
        if (!isRankPublishedForWork(workId)) {
            return Result.fail("排行榜尚未公示");
        }
        Object list = scoreService.getWorkScores(workId);
        return Result.ok(list);
    }

    @Operation(summary = "获取作品评分汇总（仅排行榜已公示时可见）")
    @GetMapping("/summary/{workId}")
    public Result<Object> getWorkSummary(@PathVariable Long workId) {
        if (!isRankPublishedForWork(workId)) {
            return Result.fail("排行榜尚未公示");
        }
        Object summaryVO = scoreService.getScoreSummary(workId);
        return Result.ok(summaryVO);
    }

    @Operation(summary = "获取批次评分汇总（仅排行榜已公示时可见）")
    @GetMapping("/batch/{batchId}")
    public Result<Object> getBatchSummary(@PathVariable Long batchId) {
        if (batchId != null && !scoreBatchService.isRankPublished(batchId)) {
            return Result.fail("排行榜尚未公示");
        }
        Object list = scoreService.getBatchScoreSummary(batchId);
        return Result.ok(list);
    }

    /**
     * 检查作品所属批次的排行榜是否已公示
     */
    private boolean isRankPublishedForWork(Long workId) {
        if (workId == null) {
            return false;
        }
        Work work = workMapper.selectById(workId);
        if (work == null || work.getBatchId() == null) {
            return false;
        }
        return scoreBatchService.isRankPublished(work.getBatchId());
    }
}
