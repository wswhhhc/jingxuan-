package com.jingxuan.modules.runtime.service.impl;

import com.jingxuan.exception.BusinessException;
import com.jingxuan.modules.runtime.dto.RuntimeDataResource;
import com.jingxuan.modules.runtime.dto.RuntimeManifestDTO;
import com.jingxuan.modules.runtime.service.RuntimeDatabaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class RuntimeDatabaseServiceImpl implements RuntimeDatabaseService {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Override
    public String createSchema(Long workId) {
        String schemaName = "jingxuan_demo_" + workId;
        executeOnServer("CREATE DATABASE IF NOT EXISTS `" + schemaName + "` CHARACTER SET utf8mb4");
        return schemaName;
    }

    @Override
    public void importInitSql(String schemaName, String initSqlPath, String projectPath) {
        if (initSqlPath == null || initSqlPath.isBlank()) {
            return;
        }
        Path sqlFile = Paths.get(projectPath).resolve(initSqlPath).normalize();
        if (!Files.exists(sqlFile)) {
            throw new BusinessException("Init SQL file not found: " + sqlFile);
        }
        try {
            String rawSql = Files.readString(sqlFile, StandardCharsets.UTF_8);
            String cleanedSql = Arrays.stream(rawSql.split("\\R"))
                    .filter(line -> !line.trim().startsWith("--"))
                    .filter(line -> !line.trim().startsWith("#"))
                    .collect(Collectors.joining("\n"));
            String[] statements = cleanedSql.split(";");
            try (Connection connection = createSchemaConnection(schemaName);
                 Statement statement = connection.createStatement()) {
                for (String sql : statements) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty()) {
                        statement.execute(trimmed);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new BusinessException("Failed to import init SQL: " + e.getMessage());
        }
    }

    @Override
    public void dropSchema(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            return;
        }
        executeOnServer("DROP DATABASE IF EXISTS `" + schemaName + "`");
    }

    @Override
    public Integer allocateRedisDb(Long workId) {
        return Math.toIntExact((workId % 15) + 1);
    }

    @Override
    public void releaseRedisDb(Integer redisDb) {
        // TODO: 释放 Redis database index
    }

    @Override
    public boolean schemaExists(String schemaName) {
        try (Connection connection = createServerConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + schemaName + "'")) {
            return resultSet.next();
        } catch (SQLException e) {
            throw new BusinessException("Failed to check schema: " + e.getMessage());
        }
    }

    @Override
    public RuntimeDataResource allocateResources(Long workId, RuntimeManifestDTO manifest, String projectPath) {
        return RuntimeDataResource.builder()
                .mysqlSchema(createSchema(workId))
                .redisDb(allocateRedisDb(workId))
                .build();
    }

    @Override
    public void releaseResources(String schemaName, Integer redisDb) {
        releaseRedisDb(redisDb);
    }

    private void executeOnServer(String sql) {
        try (Connection connection = createServerConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new BusinessException("Database operation failed: " + e.getMessage());
        }
    }

    private Connection createServerConnection() throws SQLException {
        return DriverManager.getConnection(serverJdbcUrl(), datasourceUsername, datasourcePassword);
    }

    private Connection createSchemaConnection(String schemaName) throws SQLException {
        return DriverManager.getConnection(schemaJdbcUrl(schemaName), datasourceUsername, datasourcePassword);
    }

    private String serverJdbcUrl() {
        int questionIndex = datasourceUrl.indexOf('?');
        String params = questionIndex >= 0 ? datasourceUrl.substring(questionIndex) : "";
        String base = questionIndex >= 0 ? datasourceUrl.substring(0, questionIndex) : datasourceUrl;
        int lastSlash = base.lastIndexOf('/');
        return base.substring(0, lastSlash + 1) + params;
    }

    private String schemaJdbcUrl(String schemaName) {
        int questionIndex = datasourceUrl.indexOf('?');
        String params = questionIndex >= 0 ? datasourceUrl.substring(questionIndex) : "";
        String base = questionIndex >= 0 ? datasourceUrl.substring(0, questionIndex) : datasourceUrl;
        int lastSlash = base.lastIndexOf('/');
        return base.substring(0, lastSlash + 1) + schemaName + params;
    }
}
