package com.jingxuan.modules.runtime.service;

import com.jingxuan.modules.runtime.dto.PrepareResponseDTO;
import com.jingxuan.modules.runtime.dto.RuntimeListItemDTO;
import com.jingxuan.modules.runtime.dto.RuntimeStatusDTO;
import com.jingxuan.modules.runtime.dto.StartResponseDTO;

import java.util.List;

public interface RuntimeService {

    PrepareResponseDTO prepare(Long workId);

    StartResponseDTO start(Long workId);

    RuntimeStatusDTO status(Long workId);

    RuntimeStatusDTO heartbeat(Long workId);

    RuntimeStatusDTO stop(Long workId);

    List<RuntimeListItemDTO> list();
}
