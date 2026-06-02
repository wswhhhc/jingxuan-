package com.jingxuan.modules.runtime.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.entity.PortManage;
import com.jingxuan.mapper.PortManageMapper;
import com.jingxuan.modules.runtime.dto.PortAllocationResult;
import com.jingxuan.modules.runtime.service.RuntimePortService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RuntimePortServiceImpl implements RuntimePortService {

    private final PortManageMapper portManageMapper;

    @Value("${jingxuan.runtime.backend-port-start:9000}")
    private int backendPortStart;

    @Value("${jingxuan.runtime.backend-port-end:9099}")
    private int backendPortEnd;

    @Value("${jingxuan.runtime.frontend-port-start:10000}")
    private int frontendPortStart;

    @Value("${jingxuan.runtime.frontend-port-end:10099}")
    private int frontendPortEnd;

    @Override
    public Integer allocateBackendPort(Long workId) {
        return allocatePortByType(workId, "backend", backendPortStart, backendPortEnd);
    }

    @Override
    public Integer allocateFrontendPort(Long workId) {
        return allocatePortByType(workId, "frontend", frontendPortStart, frontendPortEnd);
    }

    @Override
    public PortAllocationResult allocatePorts(Long workId) {
        return PortAllocationResult.builder()
                .backendPort(allocateBackendPort(workId))
                .frontendPort(allocateFrontendPort(workId))
                .build();
    }

    @Override
    public void releasePorts(Long workId) {
        List<PortManage> ports = portManageMapper.selectList(
                Wrappers.<PortManage>lambdaQuery().eq(PortManage::getWorkId, workId)
        );
        for (PortManage port : ports) {
            port.setStatus(0);
            port.setWorkId(null);
            port.setAllocatedTime(null);
            portManageMapper.updateById(port);
        }
    }

    @Override
    public void releasePort(Integer port, String portType) {
        PortManage entity = portManageMapper.selectOne(
                Wrappers.<PortManage>lambdaQuery()
                        .eq(PortManage::getPortNumber, port)
                        .eq(PortManage::getPortType, portType)
                        .last("LIMIT 1")
        );
        if (entity != null) {
            entity.setStatus(0);
            entity.setWorkId(null);
            entity.setAllocatedTime(null);
            portManageMapper.updateById(entity);
        }
    }

    @Override
    public boolean isPortAvailable(Integer port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public List<Integer> getAvailablePorts(String portType) {
        List<PortManage> ports = portManageMapper.selectList(
                Wrappers.<PortManage>lambdaQuery()
                        .eq(PortManage::getPortType, portType)
                        .in(PortManage::getStatus, 0, 2)
                        .orderByAsc(PortManage::getPortNumber)
        );
        if (ports.isEmpty()) {
            return Collections.emptyList();
        }
        return ports.stream().map(PortManage::getPortNumber).collect(Collectors.toList());
    }

    private Integer allocatePortByType(Long workId, String portType, int start, int end) {
        PortManage existing = portManageMapper.selectOne(
                Wrappers.<PortManage>lambdaQuery()
                        .eq(PortManage::getWorkId, workId)
                        .eq(PortManage::getPortType, portType)
                        .last("LIMIT 1")
        );
        if (existing != null && isPortAvailable(existing.getPortNumber())) {
            existing.setStatus(1);
            existing.setAllocatedTime(LocalDateTime.now());
            portManageMapper.updateById(existing);
            return existing.getPortNumber();
        }

        List<PortManage> ports = portManageMapper.selectList(
                Wrappers.<PortManage>lambdaQuery()
                        .eq(PortManage::getPortType, portType)
                        .in(PortManage::getStatus, 0, 2)
        );
        PortManage selected = ports.stream()
                .filter(port -> isPortAvailable(port.getPortNumber()))
                .sorted(Comparator.comparing(PortManage::getPortNumber))
                .findFirst()
                .orElse(null);
        if (selected != null) {
            selected.setStatus(1);
            selected.setWorkId(workId);
            selected.setAllocatedTime(LocalDateTime.now());
            portManageMapper.updateById(selected);
            return selected.getPortNumber();
        }

        for (int port = start; port <= end; port++) {
            if (!isPortAvailable(port)) {
                continue;
            }
            PortManage created = new PortManage();
            created.setPortNumber(port);
            created.setPortType(portType);
            created.setStatus(1);
            created.setWorkId(workId);
            created.setAllocatedTime(LocalDateTime.now());
            created.setPreviewUrl(null);
            portManageMapper.insert(created);
            return port;
        }

        throw new IllegalStateException("No available " + portType + " port found in range " + start + "-" + end);
    }
}
