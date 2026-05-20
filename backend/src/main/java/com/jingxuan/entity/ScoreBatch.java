package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("score_batch")
public class ScoreBatch extends BaseEntity {

    private String batchName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String classScopes;
    private Integer status;
    private Integer rankPublished;
}
