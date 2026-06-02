package com.jingxuan.modules.runtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectScanResult {

    private Long workId;
    private String projectRoot;
    private String backendPath;
    private boolean springBootProject;
    private String pomPath;
    private String artifactId;
    private String groupId;
    private String mainClass;
    private List<String> backendConfigFiles;
    private String frontendPath;
    private String packageJsonPath;
    private String frontendFramework;
    private Map<String, String> npmScripts;
    private List<String> sqlFiles;
    private String initSqlPath;
    private List<String> shellScripts;
    private List<String> batchScripts;
    private List<String> allFiles;
}
