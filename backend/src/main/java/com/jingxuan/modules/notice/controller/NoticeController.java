package com.jingxuan.modules.notice.controller;

import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.SysNotice;
import com.jingxuan.modules.notice.dto.NoticeRequest;
import com.jingxuan.modules.notice.service.NoticeService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 公告管理接口
 */
@Tag(name = "公告管理")
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "创建公告")
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody NoticeRequest request) {
        Long publisherId = SecurityUtils.requireCurrentUserId();
        Long id = noticeService.createNotice(request, publisherId);
        return Result.ok(id);
    }

    @Operation(summary = "更新公告")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeRequest request) {
        noticeService.updateNotice(id, request);
        return Result.ok();
    }

    @Operation(summary = "发布公告")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        noticeService.publishNotice(id);
        return Result.ok();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        return Result.ok();
    }

    @Operation(summary = "分页查询公告")
    @PostMapping("/list")
    public Result<PageResult<SysNotice>> list(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) Integer status) {
        PageResult<SysNotice> pageResult = noticeService.queryNoticeList(pageNum, pageSize, status);
        return Result.ok(pageResult);
    }

    @Operation(summary = "获取已发布的公告（前台）")
    @GetMapping("/published")
    public Result<PageResult<SysNotice>> published(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<SysNotice> pageResult = noticeService.getPublishedNotices(pageNum, pageSize);
        return Result.ok(pageResult);
    }
}
