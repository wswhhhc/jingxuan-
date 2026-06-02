package com.jingxuan.modules.runtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectPrepareResult {

    private boolean valid;
    private String message;
    private String projectPath;
    private String manifestPath;
    private List<String> missingFields;
    private List<String> missingFiles;
    private ProjectScanResult scanResult;
    private RuntimeManifestDTO manifest;
}
