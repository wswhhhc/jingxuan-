package com.jingxuan.modules.log.controller;

import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.SysLog;
import com.jingxuan.modules.log.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志接口
 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @Operation(summary = "分页查询操作日志")
    @PostMapping("/list")
    public Result<PageResult<SysLog>> list(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) String action) {
        PageResult<SysLog> pageResult = logService.queryLogList(pageNum, pageSize, action, null);
        return Result.ok(pageResult);
    }
}
