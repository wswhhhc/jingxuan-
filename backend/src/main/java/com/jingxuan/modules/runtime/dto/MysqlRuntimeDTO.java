package com.jingxuan.modules.runtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MysqlRuntimeDTO {

    private Boolean enabled;
    private String initSqlPath;
    private String schemaNamePattern;
    private String usernameEnv;
    private String passwordEnv;
    private String jdbcUrlEnv;
}
