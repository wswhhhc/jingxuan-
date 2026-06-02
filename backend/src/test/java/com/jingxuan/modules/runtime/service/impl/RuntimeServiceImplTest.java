package com.jingxuan.modules.runtime.service.impl;

import com.jingxuan.modules.runtime.dto.BackendRuntimeDTO;
import com.jingxuan.modules.runtime.dto.CommandPairDTO;
import com.jingxuan.modules.runtime.dto.DatabaseRuntimeDTO;
import com.jingxuan.modules.runtime.dto.FrontendRuntimeDTO;
import com.jingxuan.modules.runtime.dto.MysqlRuntimeDTO;
import com.jingxuan.modules.runtime.dto.PortAllocationResult;
import com.jingxuan.modules.runtime.dto.ProcessStartResult;
import com.jingxuan.modules.runtime.dto.ProjectPrepareResult;
import com.jingxuan.modules.runtime.dto.RuntimeDataResource;
import com.jingxuan.modules.runtime.dto.RuntimeManifestDTO;
import com.jingxuan.modules.runtime.dto.StartResponseDTO;
import com.jingxuan.modules.runtime.entity.WorkRuntime;
import com.jingxuan.modules.runtime.mapper.WorkRuntimeMapper;
import com.jingxuan.modules.runtime.service.ProjectPrepareService;
import com.jingxuan.modules.runtime.service.RuntimeAdapter;
import com.jingxuan.modules.runtime.service.RuntimeDatabaseService;
import com.jingxuan.modules.runtime.service.RuntimeLeaseService;
import com.jingxuan.modules.runtime.service.RuntimePortService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RuntimeServiceImpl")
class RuntimeServiceImplTest {

    @Mock
    private WorkRuntimeMapper workRuntimeMapper;
    @Mock
    private ProjectPrepareService projectPrepareService;
    @Mock
    private RuntimePortService runtimePortService;
    @Mock
    private RuntimeDatabaseService runtimeDatabaseService;
    @Mock
    private RuntimeLeaseService runtimeLeaseService;
    @Mock
    private RuntimeAdapter runtimeAdapter;
    @Mock
    private RuntimeEnvironmentFactory runtimeEnvironmentFactory;
    @Mock
    private RuntimeHealthProbe runtimeHealthProbe;

    @Spy
    @InjectMocks
    private RuntimeServiceImpl runtimeService;

    @Test
    @DisplayName("start should persist running runtime when dependencies succeed")
    void startShouldPersistRunningRuntime() {
        WorkRuntime runtime = new WorkRuntime();
        runtime.setId(1L);
        runtime.setWorkId(12L);
        runtime.setStatus("prepared");
        runtime.setRuntimeType("windows_process");
        runtime.setProjectPath("D:/demo/project");
        runtime.setManifestPath("D:/demo/project/jingxuan-demo.yml");

        RuntimeManifestDTO manifest = RuntimeManifestDTO.builder()
                .backend(BackendRuntimeDTO.builder()
                        .healthPath("/")
                        .artifactPath("target/demo.jar")
                        .startCommand(CommandPairDTO.builder().windows("java -jar ${JAR_PATH}").build())
                        .build())
                .frontend(FrontendRuntimeDTO.builder()
                        .startCommand(CommandPairDTO.builder().windows("npm run dev -- --host 127.0.0.1 --port ${FRONTEND_PORT}").build())
                        .build())
                .database(DatabaseRuntimeDTO.builder()
                        .mysql(MysqlRuntimeDTO.builder().enabled(true).initSqlPath("sql/init.sql").build())
                        .build())
                .build();

        when(workRuntimeMapper.selectOne(any())).thenReturn(runtime);
        when(projectPrepareService.loadManifest(anyString())).thenReturn(manifest);
        when(runtimePortService.allocatePorts(anyLong())).thenReturn(
                PortAllocationResult.builder().backendPort(9001).frontendPort(10001).build()
        );
        when(runtimeDatabaseService.allocateResources(anyLong(), any(), anyString())).thenReturn(
                RuntimeDataResource.builder().mysqlSchema("jingxuan_demo_12").redisDb(2).build()
        );
        when(runtimeAdapter.startBackend(any())).thenReturn(
                ProcessStartResult.builder().pid(111L).started(true).build()
        );
        when(runtimeAdapter.startFrontend(any())).thenReturn(
                ProcessStartResult.builder().pid(222L).started(true).build()
        );
        when(runtimeEnvironmentFactory.buildBackendEnv(any(), any())).thenReturn(java.util.Map.of());
        when(runtimeEnvironmentFactory.buildFrontendEnv(any())).thenReturn(java.util.Map.of());
        doReturn(true).when(runtimeService).waitForEndpoint(anyString(), anyInt(), anyLong());

        StartResponseDTO response = runtimeService.start(12L);

        assertEquals("running", response.getStatus());
        assertEquals(9001, response.getBackendPort());
        assertEquals(10001, response.getFrontendPort());
        assertNotNull(response.getStartedAt());
        assertEquals("running", runtime.getStatus());
        assertEquals(111L, runtime.getBackendPid());
        assertEquals(222L, runtime.getFrontendPid());
        assertEquals("jingxuan_demo_12", runtime.getMysqlSchema());
        assertEquals(2, runtime.getRedisDb());
        verify(runtimeDatabaseService).importInitSql("jingxuan_demo_12", "sql/init.sql", "D:/demo/project");
        verify(workRuntimeMapper).updateById(runtime);
    }
}
