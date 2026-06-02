package com.jingxuan.modules.runtime.dto;

import com.jingxuan.modules.runtime.entity.WorkRuntime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeListItemDTO {

    private Long workId;
    private String status;
    private String previewUrl;
    private Integer backendPort;
    private Integer frontendPort;
    private String projectPath;
    private String lastAccessTime;
    private String errorMessage;

    public static RuntimeListItemDTO from(WorkRuntime runtime) {
        return RuntimeListItemDTO.builder()
                .workId(runtime.getWorkId())
                .status(runtime.getStatus())
                .previewUrl(runtime.getPreviewUrl())
                .backendPort(runtime.getBackendPort())
                .frontendPort(runtime.getFrontendPort())
                .projectPath(runtime.getProjectPath())
                .lastAccessTime(format(runtime.getLastAccessTime()))
                .errorMessage(runtime.getErrorMessage())
                .build();
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.toString();
    }
}
