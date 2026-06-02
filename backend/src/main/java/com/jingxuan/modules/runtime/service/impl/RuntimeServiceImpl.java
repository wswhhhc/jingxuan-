package com.jingxuan.modules.runtime.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.modules.runtime.dto.ProcessStartResult;
import com.jingxuan.modules.runtime.dto.PortAllocationResult;
import com.jingxuan.modules.runtime.dto.ProjectPrepareResult;
import com.jingxuan.modules.runtime.dto.PrepareResponseDTO;
import com.jingxuan.modules.runtime.dto.RuntimeDataResource;
import com.jingxuan.modules.runtime.dto.RuntimeListItemDTO;
import com.jingxuan.modules.runtime.dto.RuntimeManifestDTO;
import com.jingxuan.modules.runtime.dto.RuntimeStatusDTO;
import com.jingxuan.modules.runtime.dto.RuntimeStartContext;
import com.jingxuan.modules.runtime.dto.StartResponseDTO;
import com.jingxuan.modules.runtime.entity.WorkRuntime;
import com.jingxuan.modules.runtime.mapper.WorkRuntimeMapper;
import com.jingxuan.modules.runtime.service.ProjectPrepareService;
import com.jingxuan.modules.runtime.service.RuntimeAdapter;
import com.jingxuan.modules.runtime.service.RuntimeDatabaseService;
import com.jingxuan.modules.runtime.service.RuntimeLeaseService;
import com.jingxuan.modules.runtime.service.RuntimePortService;
import com.jingxuan.modules.runtime.service.RuntimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class RuntimeServiceImpl implements RuntimeService {

    private static final String STATUS_INVALID = "invalid";
    private static final String STATUS_PREPARED = "prepared";
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_STOPPED = "stopped";

    private final WorkRuntimeMapper workRuntimeMapper;
    private final ProjectPrepareService projectPrepareService;
    private final RuntimePortService runtimePortService;
    private final RuntimeDatabaseService runtimeDatabaseService;
    private final RuntimeLeaseService runtimeLeaseService;
    private final RuntimeAdapter runtimeAdapter;
    private final RuntimeEnvironmentFactory runtimeEnvironmentFactory;
    private final RuntimeHealthProbe runtimeHealthProbe;

    @Override
    public PrepareResponseDTO prepare(Long workId) {
        ProjectPrepareResult result = projectPrepareService.prepareProject(workId);
        WorkRuntime runtime = getOrCreateRuntime(workId);
        runtime.setStatus(result.isValid() ? STATUS_PREPARED : STATUS_INVALID);
        runtime.setRuntimeType("windows_process");
        runtime.setProjectPath(result.getProjectPath());
        runtime.setManifestPath(result.getManifestPath());
        runtime.setPrepareTime(LocalDateTime.now());
        runtime.setErrorMessage(result.isValid() ? null : result.getMessage());
        saveRuntime(runtime);
        return PrepareResponseDTO.builder()
                .status(result.isValid() ? STATUS_PREPARED : STATUS_INVALID)
                .valid(result.isValid())
                .message(result.getMessage())
                .projectPath(result.getProjectPath())
                .manifestPath(result.getManifestPath())
                .missingFields(result.getMissingFields())
                .missingFiles(result.getMissingFiles())
                .build();
    }

    @Override
    public StartResponseDTO start(Long workId) {
        WorkRuntime runtime = getOrCreateRuntime(workId);
        if (STATUS_RUNNING.equals(runtime.getStatus())
                && runtime.getBackendPid() != null
                && runtimeAdapter.isAlive(runtime.getBackendPid())
                && runtime.getFrontendPid() != null
                && runtimeAdapter.isAlive(runtime.getFrontendPid())) {
            return StartResponseDTO.from(runtime);
        }

        if (!STATUS_PREPARED.equals(runtime.getStatus())) {
            PrepareResponseDTO prepareResponse = prepare(workId);
            if (!prepareResponse.isValid()) {
                throw new BusinessException(prepareResponse.getMessage());
            }
            runtime = getOrCreateRuntime(workId);
        }

        RuntimeManifestDTO manifest = projectPrepareService.loadManifest(runtime.getManifestPath());
        PortAllocationResult ports = runtimePortService.allocatePorts(workId);
        RuntimeDataResource dataResource = runtimeDatabaseService.allocateResources(workId, manifest, runtime.getProjectPath());
        runtimeDatabaseService.importInitSql(
                dataResource.getMysqlSchema(),
                manifest.getDatabase().getMysql().getInitSqlPath(),
                runtime.getProjectPath()
        );

        RuntimeStartContext context = RuntimeStartContext.builder()
                .workId(workId)
                .projectPath(runtime.getProjectPath())
                .previewUrl("/preview/" + workId)
                .backendPort(ports.getBackendPort())
                .frontendPort(ports.getFrontendPort())
                .mysqlSchema(dataResource.getMysqlSchema())
                .redisDb(dataResource.getRedisDb())
                .manifest(manifest)
                .backendEnv(runtimeEnvironmentFactory.buildBackendEnv(ports, dataResource))
                .frontendEnv(runtimeEnvironmentFactory.buildFrontendEnv(ports))
                .build();

        ProcessStartResult backendResult = runtimeAdapter.startBackend(context);
        if (!backendResult.isStarted()) {
            cleanupFailedStart(workId, dataResource);
            throw new BusinessException("Backend process failed to start: " + backendResult.getErrorMessage());
        }

        String backendHealthPath = manifest.getBackend() != null
                && manifest.getBackend().getHealthPath() != null
                && !manifest.getBackend().getHealthPath().isBlank()
                ? manifest.getBackend().getHealthPath()
                : "/";
        if (!waitForEndpoint("http://127.0.0.1:" + ports.getBackendPort() + backendHealthPath, 20, 1000L)) {
            runtimeAdapter.stopProcess(backendResult.getPid());
            cleanupFailedStart(workId, dataResource);
            throw new BusinessException("Backend health check did not pass in time");
        }

        ProcessStartResult frontendResult = runtimeAdapter.startFrontend(context);
        if (!frontendResult.isStarted()) {
            runtimeAdapter.stopProcess(backendResult.getPid());
            cleanupFailedStart(workId, dataResource);
            throw new BusinessException("Frontend process failed to start: " + frontendResult.getErrorMessage());
        }

        if (!waitForEndpoint("http://127.0.0.1:" + ports.getFrontendPort(), 20, 1000L)) {
            runtimeAdapter.stopProcess(backendResult.getPid());
            runtimeAdapter.stopProcess(frontendResult.getPid());
            cleanupFailedStart(workId, dataResource);
            throw new BusinessException("Frontend did not become reachable in time");
        }

        runtime.setStatus(STATUS_RUNNING);
        runtime.setPreviewUrl(context.getPreviewUrl());
        runtime.setBackendPort(ports.getBackendPort());
        runtime.setFrontendPort(ports.getFrontendPort());
        runtime.setBackendPid(backendResult.getPid());
        runtime.setFrontendPid(frontendResult.getPid());
        runtime.setMysqlSchema(dataResource.getMysqlSchema());
        runtime.setRedisDb(dataResource.getRedisDb());
        runtime.setStartTime(LocalDateTime.now());
        runtime.setLastAccessTime(LocalDateTime.now());
        runtime.setErrorMessage(null);
        saveRuntime(runtime);
        return StartResponseDTO.from(runtime);
    }

    @Override
    public RuntimeStatusDTO status(Long workId) {
        WorkRuntime runtime = findRuntime(workId);
        if (runtime == null) {
            return RuntimeStatusDTO.builder()
                    .workId(workId)
                    .status(STATUS_STOPPED)
                    .message("No runtime record found")
                    .build();
        }
        if (STATUS_RUNNING.equals(runtime.getStatus())) {
            boolean backendAlive = runtime.getBackendPid() != null && runtimeAdapter.isAlive(runtime.getBackendPid());
            boolean frontendAlive = runtime.getFrontendPid() != null && runtimeAdapter.isAlive(runtime.getFrontendPid());
            if (!backendAlive || !frontendAlive) {
                runtime.setStatus("failed");
                runtime.setErrorMessage(!backendAlive ? "Backend process is not alive" : "Frontend process is not alive");
                saveRuntime(runtime);
            }
        }
        return RuntimeStatusDTO.from(runtime);
    }

    @Override
    public RuntimeStatusDTO heartbeat(Long workId) {
        WorkRuntime runtime = getOrCreateRuntime(workId);
        if (runtime.getStatus() == null) {
            runtime.setStatus(STATUS_RUNNING);
        }
        runtimeLeaseService.refreshLease(runtime);
        saveRuntime(runtime);
        return RuntimeStatusDTO.from(runtime);
    }

    @Override
    public RuntimeStatusDTO stop(Long workId) {
        WorkRuntime runtime = getOrCreateRuntime(workId);
        runtimePortService.releasePorts(workId);
        if (runtime.getBackendPid() != null) {
            runtimeAdapter.stopProcess(runtime.getBackendPid());
        }
        if (runtime.getFrontendPid() != null) {
            runtimeAdapter.stopProcess(runtime.getFrontendPid());
        }
        runtimeDatabaseService.releaseResources(runtime.getMysqlSchema(), runtime.getRedisDb());
        runtime.setStatus(STATUS_STOPPED);
        runtime.setBackendPid(null);
        runtime.setFrontendPid(null);
        runtime.setBackendPort(null);
        runtime.setFrontendPort(null);
        runtime.setStopTime(LocalDateTime.now());
        persistStoppedRuntime(runtime);
        return RuntimeStatusDTO.from(runtime);
    }

    @Override
    public List<RuntimeListItemDTO> list() {
        return workRuntimeMapper.selectList(
                        Wrappers.<WorkRuntime>lambdaQuery()
                                .orderByDesc(WorkRuntime::getUpdateTime))
                .stream()
                .map(RuntimeListItemDTO::from)
                .collect(Collectors.toList());
    }

    private WorkRuntime findRuntime(Long workId) {
        return workRuntimeMapper.selectOne(
                Wrappers.<WorkRuntime>lambdaQuery()
                        .eq(WorkRuntime::getWorkId, workId)
                        .last("LIMIT 1")
        );
    }

    private WorkRuntime getOrCreateRuntime(Long workId) {
        WorkRuntime runtime = findRuntime(workId);
        if (runtime != null) {
            return runtime;
        }
        runtime = new WorkRuntime();
        runtime.setWorkId(workId);
        runtime.setStatus(STATUS_STOPPED);
        runtime.setRuntimeType("windows_process");
        return runtime;
    }

    private void saveRuntime(WorkRuntime runtime) {
        if (runtime.getId() == null) {
            workRuntimeMapper.insert(runtime);
        } else {
            workRuntimeMapper.updateById(runtime);
        }
    }

    private void cleanupFailedStart(Long workId, RuntimeDataResource dataResource) {
        runtimePortService.releasePorts(workId);
        runtimeDatabaseService.releaseResources(dataResource.getMysqlSchema(), dataResource.getRedisDb());
    }

    private void persistStoppedRuntime(WorkRuntime runtime) {
        if (runtime.getId() == null) {
            saveRuntime(runtime);
            return;
        }
        workRuntimeMapper.update(
                null,
                Wrappers.<WorkRuntime>lambdaUpdate()
                        .eq(WorkRuntime::getId, runtime.getId())
                        .set(WorkRuntime::getStatus, runtime.getStatus())
                        .set(WorkRuntime::getBackendPid, null)
                        .set(WorkRuntime::getFrontendPid, null)
                        .set(WorkRuntime::getBackendPort, null)
                        .set(WorkRuntime::getFrontendPort, null)
                        .set(WorkRuntime::getStopTime, runtime.getStopTime())
                        .set(WorkRuntime::getErrorMessage, runtime.getErrorMessage())
        );
    }

    boolean waitForEndpoint(String url, int maxAttempts, long sleepMillis) {
        return runtimeHealthProbe.waitForEndpoint(url, maxAttempts, sleepMillis);
    }
}
