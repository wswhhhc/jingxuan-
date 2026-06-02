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
public class RuntimeStatusDTO {

    private Long workId;
    private String status;
    private String message;
    private String previewUrl;
    private Integer backendPort;
    private Integer frontendPort;
    private Boolean backendReady;
    private Boolean frontendReady;
    private String lastAccessTime;
    private String startTime;
    private String stopTime;
    private String errorMessage;

    public static RuntimeStatusDTO from(WorkRuntime runtime) {
        return RuntimeStatusDTO.builder()
                .workId(runtime.getWorkId())
                .status(runtime.getStatus())
                .message(runtime.getErrorMessage() == null ? null : runtime.getErrorMessage())
                .previewUrl(runtime.getPreviewUrl())
                .backendPort(runtime.getBackendPort())
                .frontendPort(runtime.getFrontendPort())
                .backendReady("running".equals(runtime.getStatus()) && runtime.getBackendPid() != null)
                .frontendReady("running".equals(runtime.getStatus()) && runtime.getFrontendPid() != null)
                .lastAccessTime(format(runtime.getLastAccessTime()))
                .startTime(format(runtime.getStartTime()))
                .stopTime(format(runtime.getStopTime()))
                .errorMessage(runtime.getErrorMessage())
                .build();
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.toString();
    }
}
