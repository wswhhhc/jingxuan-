package com.jingxuan.modules.work.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkAttachment;
import com.jingxuan.entity.WorkMember;
import com.jingxuan.entity.WorkPublish;
import com.jingxuan.enums.AuditStatusEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkAttachmentMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkMemberMapper;
import com.jingxuan.mapper.WorkPublishMapper;
import com.jingxuan.modules.log.service.LogService;
import com.jingxuan.modules.score.service.ScoreService;
import com.jingxuan.modules.sensitive.service.DeepSeekReviewService;
import com.jingxuan.modules.work.dto.WorkCreateRequest;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.dto.WorkListVO;
import com.jingxuan.modules.work.dto.WorkMemberDTO;
import com.jingxuan.modules.work.dto.WorkQueryRequest;
import com.jingxuan.modules.work.dto.WorkUpdateRequest;
import com.jingxuan.modules.work.service.WorkMemberService;
import com.jingxuan.modules.work.service.WorkService;
import com.jingxuan.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 作品管理 服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class WorkServiceImpl extends ServiceImpl<WorkMapper, Work> implements WorkService {

    private final SysUserMapper sysUserMapper;
    private final WorkMemberService workMemberService;
    private final WorkMemberMapper workMemberMapper;
    private final WorkAttachmentMapper workAttachmentMapper;
    private final WorkPublishMapper workPublishMapper;
    private final ScoreBatchMapper scoreBatchMapper;
    private final LogService logService;
    @org.springframework.beans.factory.annotation.Autowired
    private ScoreService scoreService;

    @org.springframework.beans.factory.annotation.Autowired
    private DeepSeekReviewService deepSeekReviewService;

    @Override
    public Long createWork(WorkCreateRequest request) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();

        // 同一学生在当前活跃批次中只能提交一个作品
        ScoreBatch activeBatch = scoreBatchMapper.selectOne(
                Wrappers.<ScoreBatch>lambdaQuery()
                        .eq(ScoreBatch::getStatus, 1)
                        .orderByDesc(ScoreBatch::getCreateTime)
                        .last("LIMIT 1"));
        // 无论是否有活跃批次，都先回填已注册成员的 studentId（两者后续用途不同：studentId 用于成员识别与权限校验，批次唯一性校验仅在有活跃批次时执行）
        if (CollectionUtil.isNotEmpty(request.getMembers())) {
            resolveMemberStudentIds(request.getMembers());
        }

        if (activeBatch != null) {
            // 检查提交者是否已有作品在该批次中
            Long submitterCount = baseMapper.selectCount(
                    Wrappers.<Work>lambdaQuery()
                            .eq(Work::getSubmitterId, currentUserId)
                            .eq(Work::getBatchId, activeBatch.getId()));
            if (submitterCount > 0) {
                throw new BusinessException("您在当前评分批次中已有作品，每个学生只能提交一个作品");
            }
            // 检查注册成员是否已在同一批次的其他作品中
            if (CollectionUtil.isNotEmpty(request.getMembers())) {
                for (WorkMemberDTO member : request.getMembers()) {
                    if (member.getStudentId() != null) {
                        Long memberCount = workMemberMapper.selectCount(
                                Wrappers.<WorkMember>lambdaQuery()
                                        .eq(WorkMember::getStudentId, member.getStudentId())
                                        .inSql(WorkMember::getWorkId,
                                                "SELECT id FROM work WHERE batch_id = " + activeBatch.getId() + " AND deleted = 0"));
                        if (memberCount > 0) {
                            throw new BusinessException("团队成员 " + member.getStudentName() + " 在当前批次中已有作品，每个学生只能参与一个作品");
                        }
                    }
                }
            }
        }

        // 内容安全审核：作品标题、简介、运行说明
        String reviewText = String.join("\n",
                request.getTitle() != null ? request.getTitle() : "",
                request.getSummary() != null ? request.getSummary() : "",
                request.getRunDesc() != null ? request.getRunDesc() : ""
        ).trim();
        if (!reviewText.isEmpty()) {
            DeepSeekReviewService.ReviewResult review = deepSeekReviewService.review(reviewText, "work");
            if (!review.isPassed()) {
                throw new BusinessException("作品内容违规：" + review.getReason());
            }
        }

        Work work = new Work();
        work.setTitle(request.getTitle());
        work.setSummary(request.getSummary());
        work.setTechStack(request.getTechStack());
        work.setAdvisor(request.getAdvisor());
        work.setCoverUrl(request.getCoverUrl());
        work.setVideoUrl(request.getVideoUrl());
        work.setRunDesc(request.getRunDesc());
        work.setStatus(AuditStatusEnum.DRAFT.getValue());
        work.setSubmitterId(currentUserId);
        if (activeBatch != null) {
            work.setBatchId(activeBatch.getId());
        }
        baseMapper.insert(work);

        // 保存团队成员
        if (CollectionUtil.isNotEmpty(request.getMembers())) {
            workMemberService.saveMembers(work.getId(), request.getMembers());
        }

        // 校验附件归属：只允许绑定未被其他作品占用的附件
        List<String> rawIds = request.getAttachmentIds();
        List<Long> attachmentIds = parseAttachmentIds(rawIds);
        log.debug("createWork: raw attachmentIds={}, parsed attachmentIds={}", rawIds, attachmentIds);
        if (CollectionUtil.isNotEmpty(attachmentIds)) {
            Long occupiedCount = workAttachmentMapper.selectCount(
                    Wrappers.<WorkAttachment>lambdaQuery()
                            .in(WorkAttachment::getId, attachmentIds)
                            .isNotNull(WorkAttachment::getWorkId)
            );
            if (occupiedCount > 0) {
                throw new BusinessException("部分附件已被其他作品绑定，请重新上传");
            }
            workAttachmentMapper.update(
                    null,
                    Wrappers.<WorkAttachment>lambdaUpdate()
                            .set(WorkAttachment::getWorkId, work.getId())
                            .in(WorkAttachment::getId, attachmentIds)
                            .isNull(WorkAttachment::getWorkId)
            );
        }

        return work.getId();
    }

    @Override
    public void updateWork(Long id, WorkUpdateRequest request) {
        Work work = baseMapper.selectById(id);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }
        int status = work.getStatus();
        if (status != AuditStatusEnum.DRAFT.getValue()
                && status != AuditStatusEnum.REJECTED.getValue()) {
            throw new BusinessException("当前状态不允许编辑");
        }
        checkOwnership(work);

        // 更新字段：null 表示前端未传，空字符串表示显式清空
        if (request.getTitle() != null) {
            work.setTitle(request.getTitle());
        }
        if (request.getSummary() != null) {
            work.setSummary(request.getSummary());
        }
        if (request.getTechStack() != null) {
            work.setTechStack(request.getTechStack());
        }
        if (request.getAdvisor() != null) {
            work.setAdvisor(request.getAdvisor());
        }
        if (request.getCoverUrl() != null) {
            work.setCoverUrl(request.getCoverUrl());
        }
        if (request.getVideoUrl() != null) {
            work.setVideoUrl(request.getVideoUrl());
        }
        if (request.getRunDesc() != null) {
            work.setRunDesc(request.getRunDesc());
        }

        // 内容安全审核（编辑后的文本，在入库前检查）
        String reviewText = String.join("\n",
                work.getTitle() != null ? work.getTitle() : "",
                work.getSummary() != null ? work.getSummary() : "",
                work.getRunDesc() != null ? work.getRunDesc() : ""
        ).trim();
        if (!reviewText.isEmpty()) {
            DeepSeekReviewService.ReviewResult review = deepSeekReviewService.review(reviewText, "work");
            if (!review.isPassed()) {
                throw new BusinessException("作品内容违规：" + review.getReason());
            }
        }

        baseMapper.updateById(work);

        // 重新保存成员
        if (request.getMembers() != null) {
            // 无论是否有活跃批次，都先回填已注册成员的 studentId（后续 saveMembers 会将其持久化到 work_member.student_id）
            resolveMemberStudentIds(request.getMembers());

            // 仅当当前作品已归属批次时，校验同批次成员唯一性。
            // 兼容历史草稿：batchId 为空的作品可继续编辑，提交时再做批次唯一性校验。
            Long batchIdForCheck = work.getBatchId();
            if (batchIdForCheck != null) {
                for (WorkMemberDTO member : request.getMembers()) {
                    if (member.getStudentId() != null) {
                        Long memberCount = workMemberMapper.selectCount(
                                Wrappers.<WorkMember>lambdaQuery()
                                        .eq(WorkMember::getStudentId, member.getStudentId())
                                        .inSql(WorkMember::getWorkId,
                                                "SELECT id FROM work WHERE id != " + id + " AND batch_id = " + batchIdForCheck + " AND deleted = 0"));
                        if (memberCount > 0) {
                            throw new BusinessException("团队成员 " + member.getStudentName() + " 在当前批次中已有作品，每个学生只能参与一个作品");
                        }
                    }
                }
            }
            workMemberService.saveMembers(id, request.getMembers());
        }

        // 差量更新附件关联
        if (request.getAttachmentIds() != null) {
            List<Long> targetIds = parseAttachmentIds(request.getAttachmentIds());

            // 查出当前作品已绑定的附件 ID
            List<Long> currentIds = workAttachmentMapper.selectList(
                            Wrappers.<WorkAttachment>lambdaQuery()
                                    .eq(WorkAttachment::getWorkId, id)
                                    .select(WorkAttachment::getId))
                    .stream()
                    .map(WorkAttachment::getId)
                    .collect(Collectors.toList());

            // 计算差量：需要解绑的（当前有，目标无）
            List<Long> removedIds = currentIds.stream()
                    .filter(cid -> !targetIds.contains(cid))
                    .collect(Collectors.toList());

            // 计算差量：需要绑定的（目标有，当前无）
            List<Long> addedIds = targetIds.stream()
                    .filter(tid -> !currentIds.contains(tid))
                    .distinct()
                    .collect(Collectors.toList());

            // 校验 addedIds 不能被其他作品占用
            if (!addedIds.isEmpty()) {
                Long occupiedCount = workAttachmentMapper.selectCount(
                        Wrappers.<WorkAttachment>lambdaQuery()
                                .in(WorkAttachment::getId, addedIds)
                                .isNotNull(WorkAttachment::getWorkId)
                                .ne(WorkAttachment::getWorkId, id)
                );
                if (occupiedCount > 0) {
                    throw new BusinessException("部分附件已被其他作品绑定，请重新上传");
                }
            }

            // 解绑 removedIds
            if (!removedIds.isEmpty()) {
                workAttachmentMapper.update(
                        null,
                        Wrappers.<WorkAttachment>lambdaUpdate()
                                .set(WorkAttachment::getWorkId, null)
                                .in(WorkAttachment::getId, removedIds)
                                .eq(WorkAttachment::getWorkId, id)
                );
            }

            // 绑定 addedIds（防御性条件：只绑定未关联或本作品的）
            if (!addedIds.isEmpty()) {
                workAttachmentMapper.update(
                        null,
                        Wrappers.<WorkAttachment>lambdaUpdate()
                                .set(WorkAttachment::getWorkId, id)
                                .in(WorkAttachment::getId, addedIds)
                                .and(w -> w.isNull(WorkAttachment::getWorkId)
                                        .or().eq(WorkAttachment::getWorkId, id))
                );
            }
        }
    }

    @Override
    public void submitWork(Long id) {
        Work work = baseMapper.selectById(id);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }
        if (work.getStatus() != AuditStatusEnum.DRAFT.getValue()
                && work.getStatus() != AuditStatusEnum.REJECTED.getValue()) {
            throw new BusinessException("仅草稿或已驳回状态的作品可以提交审核");
        }
        checkOwnership(work);

        // 检查附件数量：提交审核前至少有一个附件
        Long attachmentCount = workAttachmentMapper.selectCount(
                Wrappers.<WorkAttachment>lambdaQuery()
                        .eq(WorkAttachment::getWorkId, id));
        if (attachmentCount == null || attachmentCount == 0) {
            throw new BusinessException("请先上传附件再提交审核");
        }

        // 自动关联活跃批次（兼容旧数据——创建时未设置 batchId 的作品）
        if (work.getBatchId() == null) {
            ScoreBatch activeBatch = scoreBatchMapper.selectOne(
                    Wrappers.<ScoreBatch>lambdaQuery()
                            .eq(ScoreBatch::getStatus, 1)
                            .orderByDesc(ScoreBatch::getCreateTime)
                            .last("LIMIT 1"));
            if (activeBatch != null) {
                Long submitterCount = baseMapper.selectCount(
                        Wrappers.<Work>lambdaQuery()
                                .eq(Work::getSubmitterId, work.getSubmitterId())
                                .eq(Work::getBatchId, activeBatch.getId())
                                .ne(Work::getId, id));
                if (submitterCount > 0) {
                    throw new BusinessException("您在当前评分批次中已有作品，每个学生只能提交一个作品");
                }
                work.setBatchId(activeBatch.getId());
            }
        }

        // 内容安全审核
        String reviewText = String.join("\n",
                work.getTitle() != null ? work.getTitle() : "",
                work.getSummary() != null ? work.getSummary() : "",
                work.getRunDesc() != null ? work.getRunDesc() : ""
        ).trim();
        if (!reviewText.isEmpty()) {
            DeepSeekReviewService.ReviewResult review = deepSeekReviewService.review(reviewText, "work");
            if (!review.isPassed()) {
                throw new BusinessException("作品内容违规：" + review.getReason());
            }
        }

        work.setStatus(AuditStatusEnum.SUBMITTED.getValue());
        work.setSubmitTime(LocalDateTime.now());
        baseMapper.updateById(work);

        logService.recordAction("提交审核", "作品", id);
    }

    @Override
    public void deleteWork(Long id) {
        Work work = baseMapper.selectById(id);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }
        int status = work.getStatus();
        if (status != AuditStatusEnum.DRAFT.getValue()
                && status != AuditStatusEnum.REJECTED.getValue()) {
            throw new BusinessException("仅草稿或已驳回状态的作品可以删除");
        }
        checkOwnership(work);

        // 解除附件关联，释放附件供后续作品复用
        workAttachmentMapper.update(
                Wrappers.<WorkAttachment>lambdaUpdate()
                        .set(WorkAttachment::getWorkId, null)
                        .eq(WorkAttachment::getWorkId, id)
        );

        // 逻辑删除（基于 BaseEntity @TableLogic 注解）
        baseMapper.deleteById(id);
    }

    @Override
    public PageResult<WorkListVO> queryWorkList(WorkQueryRequest request) {
        // 1. 分页查询
        Page<Work> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Work> wrapper = Wrappers.<Work>lambdaQuery()
                .orderByDesc(Work::getUpdateTime);

        // 筛选条件
        if (request.getStatus() != null) {
            wrapper.eq(Work::getStatus, request.getStatus());
        }
        if (request.getParticipantUserId() != null) {
            appendParticipantScope(wrapper, request.getParticipantUserId());
        } else if (request.getSubmitterId() != null) {
            wrapper.eq(Work::getSubmitterId, request.getSubmitterId());
        }
        if (request.getBatchId() != null) {
            wrapper.eq(Work::getBatchId, request.getBatchId());
        }
        if (CollectionUtil.isNotEmpty(request.getExcludeWorkIds())) {
            wrapper.notIn(Work::getId, request.getExcludeWorkIds());
        }
        if (StrUtil.isNotBlank(request.getKeyword())) {
            wrapper.and(w -> w.like(Work::getTitle, request.getKeyword())
                    .or().like(Work::getSummary, request.getKeyword())
                    .or().like(Work::getTechStack, request.getKeyword())
                    .or().like(Work::getAdvisor, request.getKeyword()));
        }
        if (StrUtil.isNotBlank(request.getTechStack())) {
            wrapper.like(Work::getTechStack, request.getTechStack());
        }
        // 班级筛选：查询该班级下的所有用户，再按 submitterId 筛选
        if (request.getClassId() != null) {
            List<Long> userIds = sysUserMapper.selectList(
                    Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getClassId, request.getClassId())
                            .select(SysUser::getId))
                    .stream().map(SysUser::getId).collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                wrapper.in(Work::getSubmitterId, userIds);
            } else {
                // 无匹配用户，返回空
                return PageResult.of(List.of(), 0, request.getPageNum(), request.getPageSize());
            }
        }
        // 提交时间范围筛选
        if (StrUtil.isNotBlank(request.getSubmitTimeBegin())) {
            wrapper.ge(Work::getSubmitTime, request.getSubmitTimeBegin());
        }
        if (StrUtil.isNotBlank(request.getSubmitTimeEnd())) {
            wrapper.le(Work::getSubmitTime, request.getSubmitTimeEnd());
        }

        Page<Work> result = baseMapper.selectPage(page, wrapper);

        // 2. 转换为 VO
        List<WorkListVO> voList = result.getRecords().stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public WorkDetailVO getWorkDetail(Long id) {
        Work work = getWorkOrThrow(id);
        return buildWorkDetailVO(work);
    }

    @Override
    public WorkDetailVO getApprovedWorkDetail(Long id) {
        Work work = getWorkOrThrow(id);
        ensureApprovedForTeacherView(work);
        return buildWorkDetailVO(work);
    }

    @Override
    public WorkDetailVO getCurrentStudentWorkDetail(Long id) {
        Work work = getWorkOrThrow(id);
        checkOwnershipForView(work);
        return buildWorkDetailVO(work);
    }

    private WorkDetailVO buildWorkDetailVO(Work work) {
        Long id = work.getId();
        WorkDetailVO vo = new WorkDetailVO();
        vo.setId(work.getId());
        vo.setTitle(work.getTitle());
        vo.setSummary(work.getSummary());
        vo.setTechStack(work.getTechStack());
        vo.setAdvisor(work.getAdvisor());
        vo.setCoverUrl(work.getCoverUrl());
        vo.setVideoUrl(work.getVideoUrl());
        vo.setRunDesc(work.getRunDesc());
        vo.setStatus(work.getStatus());
        vo.setStatusLabel(AuditStatusEnum.of(work.getStatus()).getLabel());
        vo.setSubmitterId(work.getSubmitterId());
        vo.setSubmitterName(resolveSubmitterName(work.getSubmitterId()));
        vo.setSubmitTime(work.getSubmitTime());
        vo.setBatchId(work.getBatchId());

        // 成员列表
        List<WorkMember> members = workMemberService.getByWorkId(id);
        vo.setMembers(members.stream().map(this::convertToMemberDTO).collect(Collectors.toList()));

        // 附件列表
        List<WorkAttachment> attachments = workAttachmentMapper.selectList(
                Wrappers.<WorkAttachment>lambdaQuery()
                        .eq(WorkAttachment::getWorkId, id));
        vo.setAttachments(attachments);

        // 发布信息
        WorkPublish publish = workPublishMapper.selectByWorkId(id);
        if (publish != null) {
            vo.setPublishStatus(publish.getPublishStatus());
            vo.setFeatured(publish.getFeatured());
            vo.setPreviewUrl(publish.getPreviewUrl());
        }

        // 平均分（仅排行榜已公示时显示）
        if (work.getBatchId() != null) {
            ScoreBatch batch = scoreBatchMapper.selectById(work.getBatchId());
            if (batch != null && Integer.valueOf(1).equals(batch.getRankPublished())) {
                var scoreSummary = scoreService.getScoreSummary(id);
                if (scoreSummary != null) {
                    vo.setAvgScore(scoreSummary.getAvgTotal().toString());
                }
            }
        }

        return vo;
    }

    private Work getWorkOrThrow(Long id) {
        Work work = baseMapper.selectById(id);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }
        return work;
    }

    private void ensureApprovedForTeacherView(Work work) {
        if (work.getStatus() != AuditStatusEnum.APPROVED.getValue()) {
            throw new BusinessException("作品不存在");
        }
    }

    @Override
    public List<WorkListVO> getMyWorks(Long userId) {
        List<Work> list = listParticipatedWorks(userId);
        return list.stream().map(this::convertToListVO).collect(Collectors.toList());
    }

    @Override
    public List<Work> listParticipatedWorks(Long userId) {
        LambdaQueryWrapper<Work> wrapper = Wrappers.<Work>lambdaQuery()
                .orderByDesc(Work::getUpdateTime);
        appendParticipantScope(wrapper, userId);
        return baseMapper.selectList(wrapper);
    }

    // ==================== 私有方法 ====================

    /**
     * 根据 studentNo 查询 sys_user 回填 studentId，
     * 使"一人一批次仅能归属一个作品"约束覆盖前端不传 studentId 的情况。
     * studentNo 为空或查不到已注册用户时跳过，不影响未注册成员设计。
     */
    private void resolveMemberStudentIds(List<WorkMemberDTO> members) {
        if (CollectionUtil.isEmpty(members)) {
            return;
        }
        for (WorkMemberDTO member : members) {
            if (member.getStudentId() == null && StrUtil.isNotBlank(member.getStudentNo())) {
                SysUser user = sysUserMapper.findByUsername(member.getStudentNo());
                if (user != null) {
                    member.setStudentId(user.getId());
                }
            }
        }
    }

    /**
     * 根据 submitterId 查询提交人姓名
     */
    private String resolveSubmitterName(Long submitterId) {
        if (submitterId == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectById(submitterId);
        return user != null ? user.getRealName() : null;
    }

    /**
     * 追加“提交者或已注册成员”的参与范围
     */
    private void appendParticipantScope(LambdaQueryWrapper<Work> wrapper, Long userId) {
        if (userId == null) {
            return;
        }
        wrapper.and(w -> w.eq(Work::getSubmitterId, userId)
                .or()
                .inSql(Work::getId,
                        "SELECT DISTINCT work_id FROM work_member WHERE deleted = 0 AND student_id = " + userId));
    }

    /**
     * 校验当前用户是否为作品提交者
     */
    private void checkOwnership(Work work) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!Objects.equals(work.getSubmitterId(), currentUserId)) {
            throw new BusinessException("无权操作此作品");
        }
    }

    /**
     * 校验当前学生是否可以查看作品详情
     */
    private void checkOwnershipForView(Work work) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!Objects.equals(work.getSubmitterId(), currentUserId) && !isRegisteredWorkMember(work.getId(), currentUserId)) {
            throw new BusinessException("无权查看此作品");
        }
    }

    /**
     * 校验当前学生是否为该作品的已注册团队成员
     */
    private boolean isRegisteredWorkMember(Long workId, Long userId) {
        if (workId == null || userId == null) {
            return false;
        }
        Long count = workMemberMapper.selectCount(
                Wrappers.<WorkMember>lambdaQuery()
                        .eq(WorkMember::getWorkId, workId)
                        .eq(WorkMember::getStudentId, userId));
        return count != null && count > 0;
    }

    private List<Long> parseAttachmentIds(List<String> attachmentIds) {
        if (CollectionUtil.isEmpty(attachmentIds)) {
            return List.of();
        }
        List<Long> parsed = new ArrayList<>();
        for (String attachmentId : attachmentIds) {
            if (StrUtil.isBlank(attachmentId)) {
                continue;
            }
            try {
                parsed.add(Long.parseLong(attachmentId.trim()));
            } catch (NumberFormatException e) {
                throw new BusinessException("附件ID格式无效: " + attachmentId);
            }
        }
        return parsed;
    }

    /**
     * Work -> WorkListVO 转换
     */
    private WorkListVO convertToListVO(Work work) {
        WorkListVO vo = new WorkListVO();
        vo.setId(work.getId());
        vo.setTitle(work.getTitle());
        vo.setTechStack(work.getTechStack());
        vo.setCoverUrl(work.getCoverUrl());
        vo.setStatus(work.getStatus());
        vo.setStatusLabel(AuditStatusEnum.of(work.getStatus()).getLabel());
        vo.setSubmitterId(work.getSubmitterId());
        vo.setSubmitTime(work.getSubmitTime());
        vo.setSubmitterName(resolveSubmitterName(work.getSubmitterId()));

        // 发布信息
        WorkPublish publish = workPublishMapper.selectByWorkId(work.getId());
        if (publish != null) {
            vo.setPublishStatus(publish.getPublishStatus());
            vo.setFeatured(publish.getFeatured());
        }

        // 团队成员数
        Long count = workMemberMapper.selectCount(
                Wrappers.<WorkMember>lambdaQuery()
                        .eq(WorkMember::getWorkId, work.getId()));
        vo.setMemberCount(count != null ? count.intValue() : 0);

        return vo;
    }

    /**
     * WorkMember -> WorkMemberDTO 转换
     */
    private WorkMemberDTO convertToMemberDTO(WorkMember member) {
        WorkMemberDTO dto = new WorkMemberDTO();
        dto.setId(member.getId());
        dto.setStudentId(member.getStudentId());
        dto.setStudentName(member.getStudentName());
        dto.setStudentNo(member.getStudentNo());
        dto.setClassName(member.getClassName());
        dto.setIsLeader(member.getIsLeader());
        return dto;
    }
}
