package com.jingxuan.modules.runtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessStartResult {

    private Long pid;
    private String command;
    private String workingDirectory;
    private boolean started;
    private Integer exitCode;
    private String errorMessage;
}
