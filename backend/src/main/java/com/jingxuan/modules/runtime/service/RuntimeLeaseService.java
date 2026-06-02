package com.jingxuan.modules.runtime.service;

import com.jingxuan.modules.runtime.entity.WorkRuntime;

public interface RuntimeLeaseService {

    void refreshLease(WorkRuntime runtime);
}
