package com.jingxuan.modules.runtime.service;

import com.jingxuan.modules.runtime.dto.PortAllocationResult;

import java.util.List;

public interface RuntimePortService {

    Integer allocateBackendPort(Long workId);

    Integer allocateFrontendPort(Long workId);

    PortAllocationResult allocatePorts(Long workId);

    void releasePorts(Long workId);

    void releasePort(Integer port, String portType);

    boolean isPortAvailable(Integer port);

    List<Integer> getAvailablePorts(String portType);
}
