package com.jingxuan.modules.scorebatch.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.rank.service.RankService;
import com.jingxuan.modules.scorebatch.service.ScoreBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评分批次 服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreBatchServiceImpl extends ServiceImpl<ScoreBatchMapper, ScoreBatch> implements ScoreBatchService {

    private final WorkMapper workMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDictMapper sysDictMapper;
    private final NotificationService notificationService;
    private final RankService rankService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBatch(ScoreBatch scoreBatch) {
        if (scoreBatch.getStatus() == null) {
            scoreBatch.setStatus(1);
        }
        if (scoreBatch.getRankPublished() == null) {
            scoreBatch.setRankPublished(0);
        }
        baseMapper.insert(scoreBatch);
        return scoreBatch.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBatch(ScoreBatch scoreBatch) {
        baseMapper.updateById(scoreBatch);
    }

    @Override
    public PageResult<ScoreBatch> queryBatchList(int pageNum, int pageSize) {
        Page<ScoreBatch> page = new Page<>(pageNum, pageSize);
        Page<ScoreBatch> result = baseMapper.selectPage(page,
                Wrappers.<ScoreBatch>lambdaQuery()
                        .orderByDesc(ScoreBatch::getCreateTime));
        return PageResult.of(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    @Override
    public ScoreBatch getActiveBatch() {
        LocalDateTime now = LocalDateTime.now();
        // 优先查询状态为进行中的批次
        ScoreBatch batch = lambdaQuery()
                .eq(ScoreBatch::getStatus, 1)
                .orderByDesc(ScoreBatch::getCreateTime)
                .last("LIMIT 1")
                .one();
        if (batch != null) {
            return batch;
        }
        // 如果没有进行中的批次，查询当前时间在有效期内的批次
        return lambdaQuery()
                .le(ScoreBatch::getStartTime, now)
                .ge(ScoreBatch::getEndTime, now)
                .orderByDesc(ScoreBatch::getCreateTime)
                .last("LIMIT 1")
                .one();
    }

    @Override
    public List<ScoreBatch> getAvailableBatchesForStudent(Long userId) {
        // 获取当前学生的班级 dict_value
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getClassId() == null) {
            return Collections.emptyList();
        }
        SysDict classDict = sysDictMapper.selectById(user.getClassId());
        if (classDict == null || StrUtil.isBlank(classDict.getDictValue())) {
            return Collections.emptyList();
        }
        String userClassValue = classDict.getDictValue();

        // 查询所有进行中的批次（status=1）
        List<ScoreBatch> activeBatches = lambdaQuery()
                .eq(ScoreBatch::getStatus, 1)
                .orderByDesc(ScoreBatch::getCreateTime)
                .list();

        // 过滤出该学生班级可参与的批次
        return activeBatches.stream()
                .filter(batch -> isClassInScope(userClassValue, batch.getClassScopes()))
                .collect(Collectors.toList());
    }

    private boolean isClassInScope(String userClassValue, String classScopes) {
        if (StrUtil.isBlank(classScopes)) {
            return true; // 未设置范围则所有班级可参与
        }
        String trimmed = classScopes.trim();
        if ("全校可参与".equals(trimmed) || "全校".equals(trimmed) || "all".equalsIgnoreCase(trimmed)) {
            return true;
        }
        List<String> scopeList = parseClassScopes(trimmed);
        return scopeList.stream().anyMatch(s -> s.equals(userClassValue));
    }

    private List<String> parseClassScopes(String classScopes) {
        String trimmed = classScopes.trim();
        if (trimmed.startsWith("[")) {
            try {
                JSONArray jsonArray = JSONUtil.parseArray(trimmed);
                return jsonArray.stream()
                        .map(Object::toString)
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.toList());
            } catch (Exception ignored) {
            }
        }
        return Arrays.stream(trimmed.split("[,，]"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long id) {
        baseMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishRanking(Long batchId) {
        ScoreBatch batch = baseMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("评分批次不存在");
        }
        // 发布前校验：至少有 1 个已审核作品有评分数据
        long scoredCount = workMapper.countRankedWorksByBatch(batchId);
        if (scoredCount == 0) {
            throw new BusinessException("该批次暂无已评分的作品，无法公示排行榜");
        }

        batch.setRankPublished(1);
        baseMapper.updateById(batch);

        // 刷新排行榜缓存
        rankService.refreshRankCache(batchId);

        // 通知该批次中学生查看排行榜
        List<Work> worksInBatch = workMapper.selectList(
                Wrappers.<Work>lambdaQuery()
                        .eq(Work::getBatchId, batchId)
                        .eq(Work::getStatus, 3));
        for (Work work : worksInBatch) {
            notificationService.sendNotification(
                    work.getSubmitterId(),
                    "排行榜已公示",
                    "您的作品《" + work.getTitle() + "》的评分排名已公布，请查看排行榜",
                    "ranking",
                    batchId
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublishRanking(Long batchId) {
        ScoreBatch batch = baseMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("评分批次不存在");
        }
        batch.setRankPublished(0);
        baseMapper.updateById(batch);

        // 清除排行榜缓存，避免公示取消后仍看到旧数据
        rankService.clearRankCache(batchId);
    }

    @Override
    public boolean isRankPublished(Long batchId) {
        ScoreBatch batch = baseMapper.selectById(batchId);
        return batch != null && Integer.valueOf(1).equals(batch.getRankPublished());
    }

    @Override
    public boolean isWithinScoringPeriod(Long batchId) {
        ScoreBatch batch = baseMapper.selectById(batchId);
        if (batch == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(batch.getStartTime()) && !now.isAfter(batch.getEndTime());
    }
}
