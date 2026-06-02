package com.jingxuan.modules.runtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrontendRuntimeDTO {

    private String path;
    private CommandPairDTO installCommand;
    private CommandPairDTO buildCommand;
    private CommandPairDTO startCommand;
    private String portEnv;
    private String apiBaseUrlEnv;
}
