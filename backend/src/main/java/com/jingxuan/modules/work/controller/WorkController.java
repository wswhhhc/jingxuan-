package com.jingxuan.modules.work.controller;

import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.modules.work.dto.WorkCreateRequest;
import com.jingxuan.modules.work.dto.WorkQueryRequest;
import com.jingxuan.modules.work.dto.WorkUpdateRequest;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.dto.WorkListVO;
import com.jingxuan.modules.work.service.WorkService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/work")
@RequiredArgsConstructor
@Tag(name = "作品管理")
public class WorkController {

    private final WorkService workService;

    @PostMapping("/create")
    @Operation(summary = "创建作品")
    public Result<Long> create(@RequestBody @Valid WorkCreateRequest request) {
        Long workId = workService.createWork(request);
        return Result.ok(workId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新作品")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody @Valid WorkUpdateRequest request) {
        workService.updateWork(id, request);
        return Result.ok();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审核")
    public Result<Void> submitForAudit(@PathVariable Long id) {
        workService.submitWork(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除作品")
    public Result<Void> delete(@PathVariable Long id) {
        workService.deleteWork(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取作品详情")
    public Result<WorkDetailVO> detail(@PathVariable Long id) {
        WorkDetailVO workDetailVO = workService.getWorkDetail(id);
        return Result.ok(workDetailVO);
    }

    @PostMapping("/list")
    @Operation(summary = "查询作品列表")
    public Result<PageResult<WorkListVO>> list(@RequestBody WorkQueryRequest request) {
        PageResult<WorkListVO> pageResult = workService.queryWorkList(request);
        return Result.ok(pageResult);
    }

    @GetMapping("/my")
    @Operation(summary = "获取我的作品")
    public Result<List<WorkListVO>> myWorks() {
        Long userId = SecurityUtils.requireCurrentUserId();
        List<WorkListVO> list = workService.getMyWorks(userId);
        return Result.ok(list);
    }
}
