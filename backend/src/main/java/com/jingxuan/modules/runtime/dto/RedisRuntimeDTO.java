package com.jingxuan.modules.runtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisRuntimeDTO {

    private Boolean enabled;
    private String hostEnv;
    private String portEnv;
    private String databaseEnv;
}
