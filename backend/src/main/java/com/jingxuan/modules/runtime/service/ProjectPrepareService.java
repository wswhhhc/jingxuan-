package com.jingxuan.modules.runtime.service;

import com.jingxuan.modules.runtime.dto.ProjectPrepareResult;
import com.jingxuan.modules.runtime.dto.RuntimeManifestDTO;

public interface ProjectPrepareService {

    ProjectPrepareResult prepareProject(Long workId);

    RuntimeManifestDTO loadManifest(String manifestPath);
}
