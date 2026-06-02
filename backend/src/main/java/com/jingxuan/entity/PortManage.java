package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("port_manage")
public class PortManage extends BaseEntity {

    private Integer portNumber;
    private String portType;
    private Integer status;
    private Long workId;
    private LocalDateTime allocatedTime;
    private String previewUrl;
}
