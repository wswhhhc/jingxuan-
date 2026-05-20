package com.jingxuan.modules.port.dto;

import com.jingxuan.entity.PortManage;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PortVO {

    private Long id;
    private Integer portNumber;
    private String status;
    private Long workId;
    private String workTitle;
    private String proxyUrl;
    private LocalDateTime allocatedTime;

    public static PortVO from(PortManage entity, String workTitle) {
        PortVO vo = new PortVO();
        vo.setId(entity.getId());
        vo.setPortNumber(entity.getPortNumber());
        vo.setWorkId(entity.getWorkId());
        vo.setWorkTitle(workTitle);
        vo.setProxyUrl(entity.getPreviewUrl());
        vo.setAllocatedTime(entity.getAllocatedTime());

        Integer s = entity.getStatus();
        vo.setStatus(s != null && s == 1 ? "in_use" : "free");
        return vo;
    }
}
