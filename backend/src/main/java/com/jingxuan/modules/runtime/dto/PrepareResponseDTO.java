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
public class PrepareResponseDTO {

    private String status;
    private boolean valid;
    private String message;
    private String projectPath;
    private String manifestPath;
    private List<String> missingFields;
    private List<String> missingFiles;
}
