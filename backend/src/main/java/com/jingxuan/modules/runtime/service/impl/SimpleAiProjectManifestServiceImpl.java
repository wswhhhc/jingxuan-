package com.jingxuan.modules.runtime.service.impl;

import com.jingxuan.modules.runtime.dto.BackendRuntimeDTO;
import com.jingxuan.modules.runtime.dto.CacheRuntimeDTO;
import com.jingxuan.modules.runtime.dto.CommandPairDTO;
import com.jingxuan.modules.runtime.dto.DatabaseRuntimeDTO;
import com.jingxuan.modules.runtime.dto.FrontendRuntimeDTO;
import com.jingxuan.modules.runtime.dto.MysqlRuntimeDTO;
import com.jingxuan.modules.runtime.dto.ProjectInfoDTO;
import com.jingxuan.modules.runtime.dto.ProjectScanResult;
import com.jingxuan.modules.runtime.dto.RedisRuntimeDTO;
import com.jingxuan.modules.runtime.dto.RuntimeManifestDTO;
import com.jingxuan.modules.runtime.dto.RuntimePolicyDTO;
import com.jingxuan.modules.runtime.service.AiProjectManifestService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SimpleAiProjectManifestServiceImpl implements AiProjectManifestService {

    @Override
    public RuntimeManifestDTO generateManifest(ProjectScanResult scanResult) {
        return RuntimeManifestDTO.builder()
                .version(1)
                .project(ProjectInfoDTO.builder()
                        .name(scanResult.getArtifactId() == null ? "demo-project" : scanResult.getArtifactId())
                        .type("springboot-web")
                        .description("AI-generated runtime manifest skeleton")
                        .build())
                .backend(BackendRuntimeDTO.builder()
                        .framework("springboot")
                        .path(scanResult.getBackendPath())
                        .buildCommand(CommandPairDTO.builder()
                                .windows("mvn -DskipTests package")
                                .linux("mvn -DskipTests package")
                                .build())
                        .artifactPath("target/*.jar")
                        .startCommand(CommandPairDTO.builder()
                                .windows("java -jar ${JAR_PATH}")
                                .linux("java -jar ${JAR_PATH}")
                                .build())
                        .healthPath("/")
                        .portEnv("BACKEND_PORT")
                        .build())
                .frontend(FrontendRuntimeDTO.builder()
                        .path(scanResult.getFrontendPath())
                        .installCommand(CommandPairDTO.builder()
                                .windows("npm install")
                                .linux("npm install")
                                .build())
                        .startCommand(CommandPairDTO.builder()
                                .windows("npm run dev -- --host 127.0.0.1 --port ${FRONTEND_PORT}")
                                .linux("npm run dev -- --host 0.0.0.0 --port ${FRONTEND_PORT}")
                                .build())
                        .portEnv("FRONTEND_PORT")
                        .apiBaseUrlEnv("VITE_API_BASE_URL")
                        .build())
                .database(DatabaseRuntimeDTO.builder()
                        .mysql(MysqlRuntimeDTO.builder()
                                .enabled(Boolean.TRUE)
                                .initSqlPath(scanResult.getInitSqlPath())
                                .schemaNamePattern("jingxuan_demo_${WORK_ID}")
                                .usernameEnv("DB_USERNAME")
                                .passwordEnv("DB_PASSWORD")
                                .jdbcUrlEnv("SPRING_DATASOURCE_URL")
                                .build())
                        .build())
                .cache(CacheRuntimeDTO.builder()
                        .redis(RedisRuntimeDTO.builder()
                                .enabled(Boolean.TRUE)
                                .hostEnv("SPRING_REDIS_HOST")
                                .portEnv("SPRING_REDIS_PORT")
                                .databaseEnv("SPRING_REDIS_DATABASE")
                                .build())
                        .build())
                .runtime(RuntimePolicyDTO.builder()
                        .previewPath("/preview/${WORK_ID}")
                        .idleTimeoutMinutes(5)
                        .requiredFiles(List.of("backend/pom.xml", "frontend/package.json"))
                        .build())
                .build();
    }
}
