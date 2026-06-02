package com.jingxuan.modules.runtime.service.impl;

import com.jingxuan.modules.runtime.dto.PortAllocationResult;
import com.jingxuan.modules.runtime.dto.RuntimeDataResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
class RuntimeEnvironmentFactory {

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private String redisPort;

    Map<String, String> buildBackendEnv(PortAllocationResult ports, RuntimeDataResource dataResource) {
        Map<String, String> env = new HashMap<>();
        env.put("SERVER_PORT", String.valueOf(ports.getBackendPort()));
        env.put("SPRING_DATASOURCE_URL", "jdbc:mysql://localhost:3306/" + dataResource.getMysqlSchema());
        env.put("SPRING_DATASOURCE_USERNAME", datasourceUsername);
        env.put("SPRING_DATASOURCE_PASSWORD", datasourcePassword);
        env.put("SPRING_DATA_REDIS_HOST", redisHost);
        env.put("SPRING_DATA_REDIS_PORT", redisPort);
        env.put("SPRING_DATA_REDIS_DATABASE", String.valueOf(dataResource.getRedisDb()));
        return env;
    }

    Map<String, String> buildFrontendEnv(PortAllocationResult ports) {
        Map<String, String> env = new HashMap<>();
        env.put("FRONTEND_PORT", String.valueOf(ports.getFrontendPort()));
        env.put("VITE_API_BASE_URL", "http://127.0.0.1:" + ports.getBackendPort());
        return env;
    }
}
