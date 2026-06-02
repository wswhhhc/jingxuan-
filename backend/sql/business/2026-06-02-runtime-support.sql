USE jingxuan;

ALTER TABLE port_manage
    ADD COLUMN IF NOT EXISTS port_type VARCHAR(20) NOT NULL DEFAULT 'backend' COMMENT '端口类型 backend/frontend';

CREATE TABLE IF NOT EXISTS work_runtime (
    id BIGINT NOT NULL COMMENT '运行记录ID',
    work_id BIGINT NOT NULL COMMENT '作品ID',
    status VARCHAR(20) NOT NULL COMMENT 'invalid/prepared/starting/running/failed/stopped',
    runtime_type VARCHAR(30) NOT NULL DEFAULT 'windows_process' COMMENT 'windows_process/docker',
    project_path VARCHAR(500) NOT NULL COMMENT '项目解压目录',
    manifest_path VARCHAR(500) DEFAULT NULL COMMENT 'manifest路径',
    backend_port INT DEFAULT NULL COMMENT '后端端口',
    frontend_port INT DEFAULT NULL COMMENT '前端端口',
    backend_pid BIGINT DEFAULT NULL COMMENT '后端进程PID',
    frontend_pid BIGINT DEFAULT NULL COMMENT '前端进程PID',
    preview_url VARCHAR(500) DEFAULT NULL COMMENT '平台预览地址',
    mysql_schema VARCHAR(100) DEFAULT NULL COMMENT '独立数据库名',
    redis_db INT DEFAULT NULL COMMENT 'Redis database',
    prepare_time DATETIME DEFAULT NULL COMMENT '准备完成时间',
    start_time DATETIME DEFAULT NULL COMMENT '启动时间',
    stop_time DATETIME DEFAULT NULL COMMENT '停止时间',
    last_access_time DATETIME DEFAULT NULL COMMENT '最后访问时间',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_id (work_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品运行状态表';
