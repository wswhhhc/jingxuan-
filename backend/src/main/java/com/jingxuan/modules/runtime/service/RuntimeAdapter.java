package com.jingxuan.modules.runtime.service;

import com.jingxuan.modules.runtime.dto.ProcessStartResult;
import com.jingxuan.modules.runtime.dto.RuntimeStartContext;

public interface RuntimeAdapter {

    ProcessStartResult startBackend(RuntimeStartContext context);

    ProcessStartResult startFrontend(RuntimeStartContext context);

    boolean isAlive(Long pid);

    void stopProcess(Long pid);
}
