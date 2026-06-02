package com.jingxuan.modules.runtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeStartContext {

    private Long workId;
    private String projectPath;
    private String previewUrl;
    private Integer backendPort;
    private Integer frontendPort;
    private String mysqlSchema;
    private Integer redisDb;
    private RuntimeManifestDTO manifest;
    private Map<String, String> backendEnv;
    private Map<String, String> frontendEnv;
}
