-- Run this SQL to create the required tables
-- CREATE DATABASE IF NOT EXISTS kcschedule_example DEFAULT CHARSET utf8mb4;
-- USE kcschedule_example;

CREATE TABLE IF NOT EXISTS kcschedule_service_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    method_name VARCHAR(255) COMMENT 'Method signature',
    description VARCHAR(500) COMMENT 'Business description',
    success     INT DEFAULT 0 COMMENT '0=success, 1=failure',
    duration    BIGINT COMMENT 'Execution time (ms)',
    params      TEXT COMMENT 'Request parameters (JSON)',
    result      TEXT COMMENT 'Return value (JSON)',
    error       TEXT COMMENT 'Error message',
    timestamp   DATETIME COMMENT 'Execution time',
    INDEX idx_timestamp (timestamp),
    INDEX idx_method_name (method_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Service method execution log';

CREATE TABLE IF NOT EXISTS kcschedule_task_switch_log (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_name      VARCHAR(255) NOT NULL COMMENT 'Task key',
    scheduled_name VARCHAR(255) COMMENT 'Task display name',
    operation      VARCHAR(20) NOT NULL COMMENT '操作类型：启用/停用',
    reason         VARCHAR(500) COMMENT 'Reason',
    operator       VARCHAR(100) COMMENT 'Operator',
    operate_time   DATETIME NOT NULL COMMENT 'Operation time',
    INDEX idx_task_name (task_name),
    INDEX idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Task enable/disable switch log';
