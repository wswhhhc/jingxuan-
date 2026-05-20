package com.jingxuan.modules.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkPublish;
import com.jingxuan.exception.NotFoundException;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkMemberMapper;
import com.jingxuan.mapper.WorkPublishMapper;
import com.jingxuan.mapper.WorkScoreMapper;
import com.jingxuan.modules.dict.service.DictService;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.dto.WorkListVO;
import com.jingxuan.modules.work.service.WorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 前台展示 API 适配 — 为前端提供公开的作品浏览接口
 */
@Tag(name = "前台展示API适配")
@RestController
@RequiredArgsConstructor
public class PublicApiController {

    private final WorkService workService;
    private final WorkPublishMapper workPublishMapper;
    private final WorkScoreMapper workScoreMapper;
    private final WorkMapper workMapper;
    private final WorkMemberMapper workMemberMapper;
    private final SysUserMapper sysUserMapper;
    private final DictService dictService;

    @Operation(summary = "获取已发布作品列表")
    @GetMapping("/public/works")
    public Result<PageResult<WorkListVO>> getPublishedWorks(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String techStack,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime submitTimeBegin,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime submitTimeEnd) {

        // 查询所有已发布的 workId
        List<WorkPublish> published = workPublishMapper.selectList(
                Wrappers.<WorkPublish>lambdaQuery()
                        .eq(WorkPublish::getPublishStatus, 1));
        List<Long> workIds = published.stream()
                .map(WorkPublish::getWorkId)
                .collect(Collectors.toList());
        if (workIds.isEmpty()) {
            return Result.ok(PageResult.of(List.of(), 0, page, size));
        }

        // 查询发布信息映射
        java.util.Map<Long, WorkPublish> publishMap = published.stream()
                .collect(Collectors.toMap(WorkPublish::getWorkId, p -> p));

        // 分页查询已审核通过的作品
        Page<Work> workPage = new Page<>(page, size);
        LambdaQueryWrapper<Work> wrapper = Wrappers.<Work>lambdaQuery()
                .in(Work::getId, workIds)
                .eq(Work::getStatus, 3)
                .orderByDesc(Work::getSubmitTime);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Work::getTitle, keyword)
                    .or().like(Work::getSummary, keyword));
        }
        if (techStack != null && !techStack.isBlank()) {
            wrapper.like(Work::getTechStack, techStack);
        }
        // 班级筛选：通过 sys_user.class_id 找到对应的提交者
        if (classId != null) {
            List<Long> userIds = sysUserMapper.selectList(
                    Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getClassId, classId)
                            .select(SysUser::getId))
                    .stream().map(SysUser::getId).collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                wrapper.in(Work::getSubmitterId, userIds);
            } else {
                // 无匹配用户，返回空
                return Result.ok(PageResult.of(List.of(), 0, page, size));
            }
        }
        // 提交时间范围筛选
        if (submitTimeBegin != null) {
            wrapper.ge(Work::getSubmitTime, submitTimeBegin);
        }
        if (submitTimeEnd != null) {
            wrapper.le(Work::getSubmitTime, submitTimeEnd);
        }
        Page<Work> result = workMapper.selectPage(workPage, wrapper);

        // 转换为 VO（含 featured 标记）
        List<WorkListVO> voList = result.getRecords().stream()
                .map(work -> {
                    WorkListVO vo = new WorkListVO();
                    vo.setId(work.getId());
                    vo.setTitle(work.getTitle());
                    vo.setTechStack(work.getTechStack());
                    vo.setCoverUrl(work.getCoverUrl());
                    vo.setStatus(work.getStatus());
                    vo.setSubmitTime(work.getSubmitTime());
                    // 发布信息
                    WorkPublish pub = publishMap.get(work.getId());
                    if (pub != null) {
                        vo.setPublishStatus(pub.getPublishStatus());
                        vo.setFeatured(pub.getFeatured());
                    }
                    // 团队成员数
                    Long count = workMemberMapper.selectCount(
                            Wrappers.<com.jingxuan.entity.WorkMember>lambdaQuery()
                                    .eq(com.jingxuan.entity.WorkMember::getWorkId, work.getId()));
                    vo.setMemberCount(count != null ? count.intValue() : 0);
                    return vo;
                })
                .collect(Collectors.toList());

        return Result.ok(PageResult.of(voList, result.getTotal(),
                result.getCurrent(), result.getSize()));
    }

    @Operation(summary = "获取班级列表（公开）")
    @GetMapping("/public/classes")
    public Result<List<SysDict>> getPublicClassList() {
        return Result.ok(dictService.getByType("class"));
    }

    @Operation(summary = "获取已发布作品详情")
    @GetMapping("/public/works/{id}")
    public Result<WorkDetailVO> getPublishedWorkDetail(@PathVariable Long id) {
        // 校验作品已发布（publishStatus = 1）
        WorkPublish publish = workPublishMapper.selectOne(
                Wrappers.<WorkPublish>lambdaQuery()
                        .eq(WorkPublish::getWorkId, id)
                        .eq(WorkPublish::getPublishStatus, 1));
        if (publish == null) {
            throw new NotFoundException("作品不存在或未发布");
        }
        WorkDetailVO vo = workService.getWorkDetail(id);
        return Result.ok(vo);
    }
}
