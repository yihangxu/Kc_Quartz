-- KCSchedule Service Log Table
-- Used by @ServiceLog annotation to persist method execution logs
CREATE TABLE IF NOT EXISTS kcschedule_service_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    method_name  VARCHAR(255)   COMMENT 'Method signature',
    description  VARCHAR(500)   COMMENT 'Business description from @ServiceLog',
    success      INT DEFAULT 0  COMMENT '0=success, 1=failure',
    duration     BIGINT         COMMENT 'Execution time in milliseconds',
    params       TEXT           COMMENT 'Method parameters (JSON)',
    result       TEXT           COMMENT 'Return value (JSON)',
    error        TEXT           COMMENT 'Error message if failed',
    timestamp    DATETIME       COMMENT 'Execution timestamp',
    INDEX idx_timestamp (timestamp),
    INDEX idx_method_name (method_name),
    INDEX idx_success (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KCSchedule service method execution log';

-- KCSchedule Task Switch Log Table
-- Records enable/disable operations for scheduled tasks
CREATE TABLE IF NOT EXISTS kcschedule_task_switch_log (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_name        VARCHAR(255)   NOT NULL COMMENT 'Task key (e.g. TaskClassmethod)',
    scheduled_name   VARCHAR(255)   COMMENT 'Task display name',
    operation        VARCHAR(20)    NOT NULL COMMENT '操作类型：启用/停用',
    reason           VARCHAR(500)   COMMENT '启停原因',
    operator         VARCHAR(100)   COMMENT '操作人',
    operate_time     DATETIME       NOT NULL COMMENT '操作时间',
    INDEX idx_task_name (task_name),
    INDEX idx_operation (operation),
    INDEX idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KCSchedule task enable/disable switch log';
