package com.jingxuan.modules.scorebatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.ScoreBatch;

public interface ScoreBatchService extends IService<ScoreBatch> {

    /**
     * 创建评分批次
     */
    Long createBatch(ScoreBatch scoreBatch);

    /**
     * 更新评分批次
     */
    void updateBatch(ScoreBatch scoreBatch);

    /**
     * 分页查询批次列表
     */
    PageResult<ScoreBatch> queryBatchList(int pageNum, int pageSize);

    /**
     * 获取当前进行中的批次
     */
    ScoreBatch getActiveBatch();

    /**
     * 检查是否在评分有效期内
     */
    boolean isWithinScoringPeriod(Long batchId);

    /**
     * 删除评分批次（逻辑删除）
     */
    void deleteBatch(Long id);

    /**
     * 公示排行榜
     */
    void publishRanking(Long batchId);

    /**
     * 取消排行榜公示
     */
    void unpublishRanking(Long batchId);

    /**
     * 检查排行榜是否已公示
     */
    boolean isRankPublished(Long batchId);

    /**
     * 保存批次通知内容
     */
    void saveNotice(Long batchId, String noticeTitle, String noticeContent);

    /**
     * 发布批次通知：给班级范围内所有学生发送通知
     */
    void publishNotice(Long batchId);
}
