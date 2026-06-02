package com.jingxuan.modules.runtime.service;

import com.jingxuan.modules.runtime.dto.ProjectScanResult;
import com.jingxuan.modules.runtime.dto.RuntimeManifestDTO;

public interface AiProjectManifestService {

    RuntimeManifestDTO generateManifest(ProjectScanResult scanResult);
}
