package com.jingxuan.modules.task.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.StudentTask;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.StudentTaskMapper;
import com.jingxuan.modules.task.service.StudentTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentTaskServiceImpl extends ServiceImpl<StudentTaskMapper, StudentTask> implements StudentTaskService {

    private final ScoreBatchMapper scoreBatchMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreateTasks(List<Long> userIds, Long batchId, String title, String content) {
        for (Long userId : userIds) {
            // 避免重复创建
            StudentTask existing = getByUserAndBatch(userId, batchId);
            if (existing != null) {
                // 已存在则更新内容和重置状态（重新发布）
                existing.setTitle(title);
                existing.setContent(content);
                existing.setStatus(0);
                existing.setWorkId(null);
                baseMapper.updateById(existing);
                continue;
            }
            StudentTask task = new StudentTask();
            task.setUserId(userId);
            task.setBatchId(batchId);
            task.setTitle(title);
            task.setContent(content);
            task.setStatus(0);
            baseMapper.insert(task);
        }
        log.info("批量创建待办完成: batchId={}, 学生数={}", batchId, userIds.size());
    }

    @Override
    public List<StudentTask> getStudentTasks(Long userId) {
        List<StudentTask> tasks = lambdaQuery()
                .eq(StudentTask::getUserId, userId)
                .eq(StudentTask::getDeleted, 0)
                .orderByDesc(StudentTask::getCreateTime)
                .list();

        // 填充批次名称和结束时间
        if (!tasks.isEmpty()) {
            List<Long> batchIds = tasks.stream()
                    .map(StudentTask::getBatchId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, ScoreBatch> batchMap = scoreBatchMapper.selectBatchIds(batchIds).stream()
                    .collect(Collectors.toMap(ScoreBatch::getId, b -> b, (a, b) -> a));
            for (StudentTask task : tasks) {
                ScoreBatch batch = batchMap.get(task.getBatchId());
                if (batch != null) {
                    task.setBatchName(batch.getBatchName());
                    task.setEndTime(batch.getEndTime() != null
                            ? batch.getEndTime().toString().replace("T", " ")
                            : null);
                }
            }
        }

        return tasks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, Long workId) {
        StudentTask task = baseMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("待办不存在");
        }
        task.setWorkId(workId);
        task.setStatus(1);
        baseMapper.updateById(task);
        log.info("待办已完成: taskId={}, workId={}", taskId, workId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(Long workId) {
        // 根据 workId 查找待办并重置为已驳回
        StudentTask task = lambdaQuery()
                .eq(StudentTask::getWorkId, workId)
                .eq(StudentTask::getDeleted, 0)
                .one();
        if (task != null) {
            task.setStatus(2);
            baseMapper.updateById(task);
            log.info("待办已驳回: taskId={}, workId={}", task.getId(), workId);
        }
    }

    @Override
    public StudentTask getByUserAndBatch(Long userId, Long batchId) {
        return lambdaQuery()
                .eq(StudentTask::getUserId, userId)
                .eq(StudentTask::getBatchId, batchId)
                .eq(StudentTask::getDeleted, 0)
                .one();
    }
}
