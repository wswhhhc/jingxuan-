package com.jingxuan.modules.runtime.service.impl;

import com.jingxuan.modules.runtime.entity.WorkRuntime;
import com.jingxuan.modules.runtime.service.RuntimeLeaseService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RuntimeLeaseServiceImpl implements RuntimeLeaseService {

    @Override
    public void refreshLease(WorkRuntime runtime) {
        runtime.setLastAccessTime(LocalDateTime.now());
    }
}
