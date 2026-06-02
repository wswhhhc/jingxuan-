package com.jingxuan.modules.runtime.dto;

import com.jingxuan.modules.runtime.entity.WorkRuntime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartResponseDTO {

    private String status;
    private String message;
    private String previewUrl;
    private Integer backendPort;
    private Integer frontendPort;
    private String startedAt;

    public static StartResponseDTO from(WorkRuntime runtime) {
        return StartResponseDTO.builder()
                .status(runtime.getStatus())
                .message("running".equals(runtime.getStatus()) ? "项目已启动" : "项目启动中")
                .previewUrl(runtime.getPreviewUrl())
                .backendPort(runtime.getBackendPort())
                .frontendPort(runtime.getFrontendPort())
                .startedAt(runtime.getStartTime() == null ? null : runtime.getStartTime().toString())
                .build();
    }
}
