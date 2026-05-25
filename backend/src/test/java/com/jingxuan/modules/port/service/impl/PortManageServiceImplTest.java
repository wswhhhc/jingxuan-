package com.jingxuan.modules.port.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.PortManage;
import com.jingxuan.entity.Work;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.PortManageMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.port.dto.PortVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortManageServiceImpl - 端口管理服务")
class PortManageServiceImplTest {

    @Mock private PortManageMapper portManageMapper;
    @Mock private WorkMapper workMapper;

    private PortManageServiceImpl portManageService;

    @BeforeEach
    void setUp() {
        portManageService = new PortManageServiceImpl(workMapper);
        ReflectionTestUtils.setField(portManageService, "baseMapper", portManageMapper);
    }

    private PortManage createPort(Long id, Integer portNumber, Integer status, Long workId) {
        PortManage port = new PortManage();
        port.setId(id);
        port.setPortNumber(portNumber);
        port.setStatus(status);
        port.setWorkId(workId);
        return port;
    }

    @Nested
    @DisplayName("端口查询")
    class QueryPort {

        @Test
        @DisplayName("查询所有端口列表")
        void shouldQueryAllPorts() {
            PortManage port = createPort(1L, 8081, 0, null);
            Page<PortManage> pageResult = new Page<>(1, 20, 2);
            pageResult.setRecords(List.of(port, createPort(2L, 8082, 1, 10L)));
            when(portManageMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(pageResult);

            PageResult<PortVO> result = portManageService.queryPortList(1, 20, null);

            assertEquals(2, result.getTotal());
            assertEquals("free", result.getRecords().get(0).getStatus());
        }

        @Test
        @DisplayName("筛选使用中的端口")
        void shouldFilterInUse() {
            PortManage port = createPort(1L, 8081, 1, 10L);
            Page<PortManage> pageResult = new Page<>(1, 20, 1);
            pageResult.setRecords(List.of(port));
            when(portManageMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(pageResult);

            PageResult<PortVO> result = portManageService.queryPortList(1, 20, "in_use");

            assertEquals(1, result.getTotal());
            assertEquals("in_use", result.getRecords().get(0).getStatus());
        }

        @Test
        @DisplayName("筛选空闲端口")
        void shouldFilterFree() {
            PortManage port = createPort(1L, 8081, 0, null);
            Page<PortManage> pageResult = new Page<>(1, 20, 1);
            pageResult.setRecords(List.of(port));
            when(portManageMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(pageResult);

            PageResult<PortVO> result = portManageService.queryPortList(1, 20, "free");

            assertEquals(1, result.getTotal());
            assertEquals("free", result.getRecords().get(0).getStatus());
        }

        @Test
        @DisplayName("获取可用端口列表")
        void shouldGetAvailablePorts() {
            PortManage port1 = createPort(1L, 8081, 0, null);
            PortManage port2 = createPort(2L, 8082, 2, null);
            when(portManageMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(port1, port2));

            List<PortVO> available = portManageService.getAvailablePorts();

            assertEquals(2, available.size());
        }
    }

    @Nested
    @DisplayName("端口分配")
    class AllocatePort {

        @Test
        @DisplayName("成功分配空闲端口")
        void shouldAllocateFreePort() {
            PortManage port = createPort(1L, 8081, 0, null);
            when(portManageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(port);

            PortManage result = portManageService.allocatePort(10L, 8081);

            assertEquals(Integer.valueOf(1), result.getStatus());
            assertEquals(Long.valueOf(10L), result.getWorkId());
            assertNotNull(result.getAllocatedTime());
            verify(portManageMapper).updateById(port);
        }

        @Test
        @DisplayName("端口不存在抛异常")
        void shouldThrowWhenPortNotFound() {
            when(portManageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> portManageService.allocatePort(10L, 9999));
        }

        @Test
        @DisplayName("端口已被占用抛异常")
        void shouldThrowWhenPortInUse() {
            PortManage port = createPort(1L, 8081, 1, 5L);
            when(portManageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(port);

            assertThrows(BusinessException.class,
                    () -> portManageService.allocatePort(10L, 8081));
        }
    }

    @Nested
    @DisplayName("端口释放")
    class ReleasePort {

        @Test
        @DisplayName("成功释放端口")
        void shouldReleasePort() {
            PortManage port = createPort(1L, 8081, 1, 10L);
            when(portManageMapper.selectById(1L)).thenReturn(port);

            portManageService.releasePort(1L);

            assertEquals(Integer.valueOf(0), port.getStatus());
            assertNull(port.getWorkId());
            assertNull(port.getAllocatedTime());
            verify(portManageMapper).updateById(port);
        }

        @Test
        @DisplayName("端口记录不存在抛异常")
        void shouldThrowWhenPortNotFound() {
            when(portManageMapper.selectById(999L)).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> portManageService.releasePort(999L));
        }
    }
}
