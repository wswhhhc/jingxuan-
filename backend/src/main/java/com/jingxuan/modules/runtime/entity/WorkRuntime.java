package com.jingxuan.modules.runtime.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("work_runtime")
public class WorkRuntime extends BaseEntity {

    private Long workId;
    private String status;
    private String runtimeType;
    private String projectPath;
    private String manifestPath;
    private Integer backendPort;
    private Integer frontendPort;
    private Long backendPid;
    private Long frontendPid;
    private String previewUrl;
    private String mysqlSchema;
    private Integer redisDb;
    private LocalDateTime prepareTime;
    private LocalDateTime startTime;
    private LocalDateTime stopTime;
    private LocalDateTime lastAccessTime;
    private String errorMessage;
}
