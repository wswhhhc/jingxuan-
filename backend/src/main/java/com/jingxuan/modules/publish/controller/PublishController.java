package com.jingxuan.modules.publish.controller;

import com.jingxuan.common.Result;
import com.jingxuan.entity.WorkPublish;
import com.jingxuan.modules.publish.dto.FeaturedRequest;
import com.jingxuan.modules.publish.service.PublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 发布管理接口
 */
@Tag(name = "发布管理")
@RestController
@RequestMapping("/api/publish")
@RequiredArgsConstructor
public class PublishController {

    private final PublishService publishService;

    @Operation(summary = "发布作品")
    @PostMapping("/{workId}")
    public Result<Void> publish(@PathVariable Long workId) {
        publishService.publishWork(workId);
        return Result.ok();
    }

    @Operation(summary = "下线作品")
    @PostMapping("/{workId}/offline")
    public Result<Void> offline(@PathVariable Long workId) {
        publishService.offlineWork(workId);
        return Result.ok();
    }

    @Operation(summary = "设置精选")
    @PostMapping("/featured")
    public Result<Void> setFeatured(@Valid @RequestBody FeaturedRequest request) {
        publishService.setFeatured(request);
        return Result.ok();
    }

    @Operation(summary = "获取发布信息")
    @GetMapping("/{workId}")
    public Result<WorkPublish> getByWorkId(@PathVariable Long workId) {
        WorkPublish data = publishService.getByWorkId(workId);
        return Result.ok(data);
    }
}
