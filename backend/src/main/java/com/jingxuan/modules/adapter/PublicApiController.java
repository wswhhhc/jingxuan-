package com.jingxuan.modules.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkLike;
import com.jingxuan.entity.WorkPublish;
import com.jingxuan.entity.WorkTag;
import com.jingxuan.exception.NotFoundException;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.TagMapper;
import com.jingxuan.mapper.WorkLikeMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkMemberMapper;
import com.jingxuan.mapper.WorkPublishMapper;
import com.jingxuan.mapper.WorkScoreMapper;
import com.jingxuan.mapper.WorkTagMapper;
import com.jingxuan.modules.dict.service.DictService;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.dto.WorkListVO;
import com.jingxuan.modules.work.service.WorkService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final WorkLikeMapper workLikeMapper;
    private final TagMapper tagMapper;
    private final WorkTagMapper workTagMapper;

    @Operation(summary = "获取已发布作品列表")
    @GetMapping("/public/works")
    public Result<PageResult<WorkListVO>> getPublishedWorks(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String techStack,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime submitTimeBegin,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime submitTimeEnd) {

        List<WorkPublish> published = workPublishMapper.selectList(
                Wrappers.<WorkPublish>lambdaQuery()
                        .eq(WorkPublish::getPublishStatus, 1));
        List<Long> workIds = published.stream()
                .map(WorkPublish::getWorkId)
                .collect(Collectors.toList());
        if (workIds.isEmpty()) {
            return Result.ok(PageResult.of(List.of(), 0, page, size));
        }

        Map<Long, WorkPublish> publishMap = published.stream()
                .collect(Collectors.toMap(WorkPublish::getWorkId, p -> p));

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
        // 标签筛选：通过 work_tag 找到对应的 workId
        if (tagIds != null && !tagIds.isEmpty()) {
            List<Long> taggedWorkIds = workTagMapper.selectList(
                            Wrappers.<WorkTag>lambdaQuery()
                                    .in(WorkTag::getTagId, tagIds))
                    .stream().map(WorkTag::getWorkId).distinct().collect(Collectors.toList());
            if (taggedWorkIds.isEmpty()) {
                return Result.ok(PageResult.of(List.of(), 0, page, size));
            }
            wrapper.in(Work::getId, taggedWorkIds);
        }
        if (classId != null) {
            List<Long> userIds = sysUserMapper.selectList(
                    Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getClassId, classId)
                            .select(SysUser::getId))
                    .stream().map(SysUser::getId).collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                wrapper.in(Work::getSubmitterId, userIds);
            } else {
                return Result.ok(PageResult.of(List.of(), 0, page, size));
            }
        }
        if (submitTimeBegin != null) {
            wrapper.ge(Work::getSubmitTime, submitTimeBegin);
        }
        if (submitTimeEnd != null) {
            wrapper.le(Work::getSubmitTime, submitTimeEnd);
        }
        Page<Work> result = workMapper.selectPage(workPage, wrapper);

        // 获取当前用户（可能未登录）
        Long currentUserId = getCurrentUserId();

        List<WorkListVO> voList = result.getRecords().stream()
                .map(work -> {
                    WorkListVO vo = new WorkListVO();
                    vo.setId(work.getId());
                    vo.setTitle(work.getTitle());
                    vo.setTechStack(work.getTechStack());
                    vo.setCoverUrl(work.getCoverUrl());
                    vo.setStatus(work.getStatus());
                    vo.setSubmitTime(work.getSubmitTime());
                    WorkPublish pub = publishMap.get(work.getId());
                    if (pub != null) {
                        vo.setPublishStatus(pub.getPublishStatus());
                        vo.setFeatured(pub.getFeatured());
                    }
                    Long count = workMemberMapper.selectCount(
                            Wrappers.<com.jingxuan.entity.WorkMember>lambdaQuery()
                                    .eq(com.jingxuan.entity.WorkMember::getWorkId, work.getId()));
                    vo.setMemberCount(count != null ? count.intValue() : 0);
                    vo.setLikeCount(work.getLikeCount() != null ? work.getLikeCount() : 0);
                    vo.setViewCount(work.getViewCount() != null ? work.getViewCount() : 0);
                    vo.setTags(getWorkTagNames(work.getId()));
                    if (currentUserId != null) {
                        vo.setLiked(workLikeMapper.selectCount(
                                Wrappers.<WorkLike>lambdaQuery()
                                        .eq(WorkLike::getWorkId, work.getId())
                                        .eq(WorkLike::getUserId, currentUserId)) > 0);
                    }
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

    @Operation(summary = "获取标签列表（公开）")
    @GetMapping("/public/tags")
    public Result<List<com.jingxuan.entity.Tag>> getPublicTagList() {
        return Result.ok(tagMapper.selectList(
                Wrappers.<com.jingxuan.entity.Tag>lambdaQuery()
                        .eq(com.jingxuan.entity.Tag::getDeleted, 0)
                        .orderByAsc(com.jingxuan.entity.Tag::getSort)));
    }

    @Operation(summary = "获取已发布作品详情")
    @GetMapping("/public/works/{id}")
    public Result<WorkDetailVO> getPublishedWorkDetail(@PathVariable Long id) {
        WorkPublish publish = workPublishMapper.selectOne(
                Wrappers.<WorkPublish>lambdaQuery()
                        .eq(WorkPublish::getWorkId, id)
                        .eq(WorkPublish::getPublishStatus, 1));
        if (publish == null) {
            throw new NotFoundException("作品不存在或未发布");
        }
        WorkDetailVO vo = workService.getWorkDetail(id);

        // 填充 likeCount / viewCount
        Work work = workMapper.selectById(id);
        if (work != null) {
            vo.setLikeCount(work.getLikeCount() != null ? work.getLikeCount() : 0);
            vo.setViewCount(work.getViewCount() != null ? work.getViewCount() : 0);
        }
        vo.setTags(getWorkTagNames(id));

        // 当前用户是否已点赞
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            vo.setLiked(workLikeMapper.selectCount(
                    Wrappers.<WorkLike>lambdaQuery()
                            .eq(WorkLike::getWorkId, id)
                            .eq(WorkLike::getUserId, currentUserId)) > 0);
        }

        // 增加浏览计数
        workMapper.update(null, Wrappers.<Work>lambdaUpdate()
                .setSql("view_count = view_count + 1")
                .eq(Work::getId, id));
        return Result.ok(vo);
    }

    @Operation(summary = "点赞/取消点赞作品")
    @PostMapping("/works/{id}/like")
    public Result<Map<String, Object>> toggleLike(@PathVariable Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();

        Work work = workMapper.selectById(id);
        if (work == null) {
            throw new NotFoundException("作品不存在");
        }

        WorkLike existing = workLikeMapper.selectOne(
                Wrappers.<WorkLike>lambdaQuery()
                        .eq(WorkLike::getWorkId, id)
                        .eq(WorkLike::getUserId, userId));

        boolean liked;
        if (existing != null) {
            workLikeMapper.deleteById(existing.getId());
            workMapper.update(null, Wrappers.<Work>lambdaUpdate()
                    .setSql("like_count = GREATEST(like_count - 1, 0)")
                    .eq(Work::getId, id));
            liked = false;
        } else {
            WorkLike workLike = new WorkLike();
            workLike.setWorkId(id);
            workLike.setUserId(userId);
            workLikeMapper.insert(workLike);
            workMapper.update(null, Wrappers.<Work>lambdaUpdate()
                    .setSql("like_count = like_count + 1")
                    .eq(Work::getId, id));
            liked = true;
        }

        Work refreshed = workMapper.selectById(id);
        int likeCount = refreshed != null && refreshed.getLikeCount() != null
                ? refreshed.getLikeCount() : 0;

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", likeCount);
        return Result.ok(result);
    }

    @Operation(summary = "获取作品点赞状态（未登录时 liked=false）")
    @GetMapping("/public/works/{id}/like-status")
    public Result<Map<String, Object>> getLikeStatus(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean liked = workLikeMapper.selectCount(
                Wrappers.<WorkLike>lambdaQuery()
                        .eq(WorkLike::getWorkId, id)
                        .eq(WorkLike::getUserId, userId)) > 0;
        Work work = workMapper.selectById(id);
        int likeCount = work != null && work.getLikeCount() != null ? work.getLikeCount() : 0;

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", likeCount);
        return Result.ok(result);
    }

    /**
     * 获取作品关联的标签名称列表
     */
    private List<String> getWorkTagNames(Long workId) {
        List<WorkTag> workTags = workTagMapper.selectList(
                Wrappers.<WorkTag>lambdaQuery().eq(WorkTag::getWorkId, workId));
        if (workTags.isEmpty()) return Collections.emptyList();

        List<Long> tagIds = workTags.stream().map(WorkTag::getTagId).collect(Collectors.toList());
        List<com.jingxuan.entity.Tag> tags = tagMapper.selectBatchIds(tagIds);
        return tags.stream().map(com.jingxuan.entity.Tag::getName).collect(Collectors.toList());
    }

    /**
     * 安全获取当前用户ID，未登录返回 null
     */
    private Long getCurrentUserId() {
        try {
            return SecurityUtils.requireCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
