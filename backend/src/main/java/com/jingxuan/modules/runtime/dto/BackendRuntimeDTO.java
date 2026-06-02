package com.jingxuan.modules.runtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackendRuntimeDTO {

    private String framework;
    private String path;
    private CommandPairDTO buildCommand;
    private String artifactPath;
    private CommandPairDTO startCommand;
    private String healthPath;
    private String portEnv;
}
