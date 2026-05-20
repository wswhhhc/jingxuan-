package com.jingxuan.modules.prize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.entity.RewardIssue;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.RewardIssueMapper;
import com.jingxuan.modules.prize.service.RewardIssueService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RewardIssueServiceImpl extends ServiceImpl<RewardIssueMapper, RewardIssue> implements RewardIssueService {

    @Override
    public Page<RewardIssue> listByPage(int page, int size, Long rewardId) {
        LambdaQueryWrapper<RewardIssue> qw = new LambdaQueryWrapper<>();
        if (rewardId != null) qw.eq(RewardIssue::getRewardId, rewardId);
        qw.orderByDesc(RewardIssue::getCreateTime);
        return page(new Page<>(page, size), qw);
    }

    @Override
    public void issue(Long rewardId, Long workId, Long operatorId) {
        RewardIssue entity = new RewardIssue();
        entity.setRewardId(rewardId);
        entity.setWorkId(workId);
        entity.setIssueStatus(1);
        entity.setIssueTime(LocalDateTime.now());
        entity.setOperatorId(operatorId);
        save(entity);
    }

    @Override
    public void cancelIssue(Long issueId) {
        RewardIssue entity = getById(issueId);
        if (entity == null) throw new BusinessException("发放记录不存在");
        entity.setIssueStatus(0);
        entity.setIssueTime(null);
        updateById(entity);
    }

    @Override
    public List<RewardIssue> getByWorkId(Long workId) {
        return list(new LambdaQueryWrapper<RewardIssue>()
                .eq(RewardIssue::getWorkId, workId));
    }
}
