package com.jingxuan.modules.port.controller;

import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.PortManage;
import com.jingxuan.modules.port.dto.PortVO;
import com.jingxuan.modules.port.service.PortManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "端口管理")
@RestController
@RequestMapping("/admin/port")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PortManageController {

    private final PortManageService portManageService;

    @Operation(summary = "获取端口列表")
    @GetMapping("/list")
    public Result<PageResult<PortVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return Result.ok(portManageService.queryPortList(page, size, status));
    }

    @Operation(summary = "分配端口")
    @PostMapping("/allocate")
    public Result<PortVO> allocate(@RequestBody Map<String, Object> body) {
        Long workId = Long.valueOf(body.get("workId").toString());
        Integer portNumber = Integer.valueOf(body.get("portNumber").toString());
        PortManage entity = portManageService.allocatePort(workId, portNumber);
        return Result.ok(PortVO.from(entity, null));
    }

    @Operation(summary = "释放端口")
    @PostMapping("/{id}/release")
    public Result<Void> release(@PathVariable Long id) {
        portManageService.releasePort(id);
        return Result.ok();
    }

    @Operation(summary = "获取可用端口列表")
    @GetMapping("/available")
    public Result<List<PortVO>> available() {
        return Result.ok(portManageService.getAvailablePorts());
    }
}
