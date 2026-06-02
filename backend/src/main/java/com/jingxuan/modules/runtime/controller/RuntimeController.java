package com.jingxuan.modules.runtime.controller;

import com.jingxuan.common.Result;
import com.jingxuan.modules.runtime.dto.PrepareResponseDTO;
import com.jingxuan.modules.runtime.dto.RuntimeListItemDTO;
import com.jingxuan.modules.runtime.dto.RuntimeStatusDTO;
import com.jingxuan.modules.runtime.dto.StartResponseDTO;
import com.jingxuan.modules.runtime.service.RuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "作品运行管理")
@RestController
@RequestMapping("/api/runtime")
@RequiredArgsConstructor
public class RuntimeController {

    private final RuntimeService runtimeService;

    @Operation(summary = "准备项目运行环境")
    @PostMapping("/{workId}/prepare")
    public Result<PrepareResponseDTO> prepare(@PathVariable Long workId) {
        return Result.ok(runtimeService.prepare(workId));
    }

    @Operation(summary = "启动作品运行实例")
    @PostMapping("/{workId}/start")
    public Result<StartResponseDTO> start(@PathVariable Long workId) {
        return Result.ok(runtimeService.start(workId));
    }

    @Operation(summary = "查询运行状态")
    @GetMapping("/{workId}/status")
    public Result<RuntimeStatusDTO> status(@PathVariable Long workId) {
        return Result.ok(runtimeService.status(workId));
    }

    @Operation(summary = "刷新运行租约")
    @PostMapping("/{workId}/heartbeat")
    public Result<RuntimeStatusDTO> heartbeat(@PathVariable Long workId) {
        return Result.ok(runtimeService.heartbeat(workId));
    }

    @Operation(summary = "停止作品运行实例")
    @PostMapping("/{workId}/stop")
    public Result<RuntimeStatusDTO> stop(@PathVariable Long workId) {
        return Result.ok(runtimeService.stop(workId));
    }

    @Operation(summary = "管理员查看运行实例列表")
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<RuntimeListItemDTO>> list() {
        return Result.ok(runtimeService.list());
    }
}
