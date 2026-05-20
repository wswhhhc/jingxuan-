package com.jingxuan.modules.audit.controller;

import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.modules.audit.dto.AuditRequest;
import com.jingxuan.modules.audit.dto.AuditHistoryVO;
import com.jingxuan.modules.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "审核管理")
public class AuditController {

    private final AuditService auditService;

    @PostMapping("/approve")
    @Operation(summary = "审核通过")
    public Result<Void> approve(@RequestBody @Valid AuditRequest request) {
        auditService.approve(request);
        return Result.ok();
    }

    @PostMapping("/reject")
    @Operation(summary = "审核驳回")
    public Result<Void> reject(@RequestBody @Valid AuditRequest request) {
        auditService.reject(request);
        return Result.ok();
    }

    @GetMapping("/history")
    @Operation(summary = "查询审核历史")
    public Result<PageResult<AuditHistoryVO>> history(@RequestParam Long workId,
                                                       @RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<AuditHistoryVO> pageResult = auditService.queryHistory(workId, pageNum, pageSize);
        return Result.ok(pageResult);
    }
}
