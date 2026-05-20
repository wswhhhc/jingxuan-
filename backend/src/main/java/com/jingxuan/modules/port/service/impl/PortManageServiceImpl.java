package com.jingxuan.modules.port.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.PortManage;
import com.jingxuan.entity.Work;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.PortManageMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.port.dto.PortVO;
import com.jingxuan.modules.port.service.PortManageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortManageServiceImpl extends ServiceImpl<PortManageMapper, PortManage> implements PortManageService {

    private final WorkMapper workMapper;

    @Override
    public PageResult<PortVO> queryPortList(int pageNum, int pageSize, String status) {
        Page<PortManage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PortManage> wrapper = new LambdaQueryWrapper<PortManage>()
                .orderByAsc(PortManage::getPortNumber);
        if ("in_use".equals(status)) {
            wrapper.eq(PortManage::getStatus, 1);
        } else if ("free".equals(status)) {
            wrapper.in(PortManage::getStatus, 0, 2);
        }
        Page<PortManage> result = baseMapper.selectPage(page, wrapper);
        List<PortVO> voList = result.getRecords().stream()
                .map(this::toPortVO)
                .collect(Collectors.toList());
        return PageResult.of(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortManage allocatePort(Long workId, Integer portNumber) {
        PortManage port = baseMapper.selectOne(
                new LambdaQueryWrapper<PortManage>()
                        .eq(PortManage::getPortNumber, portNumber));
        if (port == null) {
            throw new BusinessException("端口 " + portNumber + " 不存在");
        }
        if (port.getStatus() == 1) {
            throw new BusinessException("端口 " + portNumber + " 已被占用");
        }

        port.setWorkId(workId);
        port.setStatus(1);
        port.setAllocatedTime(LocalDateTime.now());
        baseMapper.updateById(port);
        return port;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releasePort(Long id) {
        PortManage port = baseMapper.selectById(id);
        if (port == null) {
            throw new BusinessException("端口记录不存在");
        }
        port.setStatus(0);
        port.setWorkId(null);
        port.setAllocatedTime(null);
        baseMapper.updateById(port);
    }

    @Override
    public List<PortVO> getAvailablePorts() {
        return baseMapper.selectList(
                new LambdaQueryWrapper<PortManage>()
                        .in(PortManage::getStatus, 0, 2)
                        .orderByAsc(PortManage::getPortNumber))
                .stream()
                .map(this::toPortVO)
                .collect(Collectors.toList());
    }

    private PortVO toPortVO(PortManage entity) {
        String workTitle = null;
        if (entity.getWorkId() != null) {
            Work work = workMapper.selectById(entity.getWorkId());
            if (work != null) {
                workTitle = work.getTitle();
            }
        }
        return PortVO.from(entity, workTitle);
    }
}
