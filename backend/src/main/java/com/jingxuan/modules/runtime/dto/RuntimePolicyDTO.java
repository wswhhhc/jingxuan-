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
public class RuntimePolicyDTO {

    private String previewPath;
    private Integer idleTimeoutMinutes;
    private List<String> requiredFiles;
}
