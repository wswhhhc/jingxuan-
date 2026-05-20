package com.jingxuan.modules.port.service;

import com.jingxuan.common.PageResult;
import com.jingxuan.entity.PortManage;
import com.jingxuan.modules.port.dto.PortVO;

import java.util.List;

public interface PortManageService {

    PageResult<PortVO> queryPortList(int pageNum, int pageSize, String status);

    PortManage allocatePort(Long workId, Integer portNumber);

    void releasePort(Long id);

    List<PortVO> getAvailablePorts();
}
