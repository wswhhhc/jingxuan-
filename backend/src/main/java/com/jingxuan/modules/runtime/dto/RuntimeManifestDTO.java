package com.jingxuan.modules.runtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeManifestDTO {

    private Integer version;
    private ProjectInfoDTO project;
    private BackendRuntimeDTO backend;
    private FrontendRuntimeDTO frontend;
    private DatabaseRuntimeDTO database;
    private CacheRuntimeDTO cache;
    private RuntimePolicyDTO runtime;
}
