package com.jingxuan.modules.runtime.service;

import com.jingxuan.modules.runtime.dto.RuntimeDataResource;
import com.jingxuan.modules.runtime.dto.RuntimeManifestDTO;

public interface RuntimeDatabaseService {

    String createSchema(Long workId);

    void importInitSql(String schemaName, String initSqlPath, String projectPath);

    void dropSchema(String schemaName);

    Integer allocateRedisDb(Long workId);

    void releaseRedisDb(Integer redisDb);

    boolean schemaExists(String schemaName);

    RuntimeDataResource allocateResources(Long workId, RuntimeManifestDTO manifest, String projectPath);

    void releaseResources(String schemaName, Integer redisDb);
}
