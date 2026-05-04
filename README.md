<p align="center">
  <img src="https://img.shields.io/badge/Java-8+-green?style=flat-square" alt="Java">
  <img src="https://img.shields.io/badge/Spring_Boot-2.x/3.x-brightgreen?style=flat-square" alt="Spring Boot">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License">
</p>

<h1 align="center">KCSchedule</h1>

<p align="center">
  <b>轻量级 Spring Boot 定时任务可视化管理框架</b><br>
  <b>Lightweight Spring Boot Scheduled Task Management Framework with Web UI</b>
</p>

<p align="center">
  <a href="#中文文档">中文文档</a> | <a href="#english-documentation">English Documentation</a>
</p>

---

<a id="中文文档"></a>
# 中文文档

## 为什么需要 KCSchedule？

Spring 的 `@Scheduled` 注解让定时任务开发变得简单，但在生产环境中你很快会遇到这些问题：

- **无法运行时控制** —— 启停任务必须重启应用
- **缺乏可观测性** —— 看不到任务的执行状态、历史和错误
- **没有告警机制** —— 任务挂了，直到用户投诉才知道
- **任务管理混乱** —— 任务越来越多，缺乏分组和管理手段

现有的定时任务框架如 [XXL-JOB](https://github.com/xuxueli/xxl-job)、[PowerJob](https://github.com/PowerJob/PowerJob) 功能强大，但需要**单独部署调度中心**。对于单体应用或中小型系统来说，这太重了。

> 市面上**几乎没有**专门针对 Spring Boot 单体应用的轻量级定时任务管理框架。KCSchedule 就是为了填补这个空白而生 —— **零部署、注解驱动、开箱即用**。

## 核心特性

- **注解驱动** —— 用 `@KCScheduled` 替换 `@Scheduled`，其他全自动
- **内置 Web 管理界面** —— 任务树形视图、执行历史、手动触发、启停控制
- **运行时动态控制** —— 通过 Redis 实时启停任务，无需重启
- **AOP 执行监控** —— 自动记录执行耗时、成功/失败、错误堆栈
- **多渠道告警** —— 可扩展的通知系统（内置邮件通知）
- **任务分组** —— 通过 `dirName` 按目录层级组织任务
- **服务日志** —— `@ServiceLog` 注解记录方法执行日志，支持敏感字段脱敏
- **启停审计** —— 启停任务时记录操作人和原因，可追溯

## 使用前提

> **重要：本项目基于 Spring Boot 构建**，且依赖 Spring 的 `@Scheduled` 注解机制。

1. **必须是 Spring Boot 项目** —— 框架在 Spring Boot 的 `ApplicationReadyEvent` 事件中扫描 `@KCScheduled` 注解，纯 Spring Framework 项目（非 Boot）需要自行适配启动流程
2. **必须使用注解方式定义定时任务** —— 如果你用的是 XML 配置定时任务、`Timer`、`Quartz` 直连等方式，KCSchedule 无法管理它们。只有通过 `@Scheduled`（或本项目提供的 `@KCScheduled`）定义的任务才能被识别和管理
3. **项目需要引入 Redis** —— 任务的运行状态和执行历史存储在 Redis 中，这是唯一的运行时外部依赖

> 💡 如果你已经有大量 `@Scheduled` 注解的任务，直接替换为 `@KCScheduled` 即可接入，几乎零迁移成本。

## 与主流框架对比

| 特性 | KCSchedule | XXL-JOB | PowerJob |
|------|-----------|---------|----------|
| 部署方式 | 嵌入应用（JAR） | 需独立部署调度中心 | 需独立部署调度中心 |
| 代码改动 | 替换注解 | 实现 JobHandler | 实现处理器 |
| 分布式支持 | 否（单应用） | 是 | 是 |
| Web 管理界面 | 内置 | 内置 | 内置 |
| 告警通知 | 邮件（可扩展） | 邮件 | 邮件/钉钉/企业微信 |
| 学习成本 | 极低 | 中等 | 中等 |
| 适用场景 | 单体应用任务管理 | 分布式任务调度 | 复杂工作流编排 |

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.kcschedule</groupId>
    <artifactId>kcschedule</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 添加配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379

kcschedule:
  scan-base-package: com.yourpackage  # 扫描 @KCScheduled 任务的基础包
  max-pool: 20                         # 每个任务最大保留的执行历史条数
  web:
    username: admin                    # Web 管理界面登录用户名
    password: admin                    # Web 管理界面登录密码
  notify:
    channels: email                    # 启用邮件通知
    mail:
      host: smtp.example.com
      port: 465
      username: your@email.com
      password: your-password
      recipients: receiver@example.com  # 收件人（多个用逗号分隔）
  queue-name: kcschedule:error         # Redis 告警消息队列 Key
```

### 3. 使用 @KCScheduled

将 `@Scheduled` 替换为 `@KCScheduled`：

```java
@Component
public class MyTasks {

    @KCScheduled(cron = "0 0/5 * * * ?",
                 cronName = "数据同步",
                 cronDocs = "每5分钟从外部系统同步数据",
                 dirName = "数据|同步")
    public void syncData() {
        // 你的业务逻辑
    }

    @KCScheduled(cron = "0 0 2 * * ?",
                 cronName = "日报生成",
                 cronDocs = "每天凌晨2点生成日报",
                 dirName = "报表",
                 isRun = false)  // 初始为停用状态
    public void generateReport() {
        // 你的业务逻辑
    }
}
```

### 4. 访问管理界面

浏览器打开 `http://localhost:{port}/html/quartzInfo.html`

### 5. （可选）启用服务日志持久化

如果需要使用 `@ServiceLog` 将方法执行日志持久化到数据库：

**添加 MyBatis 依赖**（你的项目中）：
```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
</dependency>
```

**创建日志表**：
```sql
CREATE TABLE kcschedule_service_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    method_name VARCHAR(255) COMMENT '方法签名',
    description VARCHAR(500) COMMENT '业务描述',
    success     INT DEFAULT 0  COMMENT '0=成功 1=失败',
    duration    BIGINT COMMENT '执行耗时(毫秒)',
    params      TEXT COMMENT '请求参数(JSON)',
    result      TEXT COMMENT '返回值(JSON)',
    error       TEXT COMMENT '错误信息',
    timestamp   DATETIME COMMENT '执行时间',
    INDEX idx_timestamp (timestamp),
    INDEX idx_method_name (method_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KCSchedule 服务方法执行日志';
```

建表脚本也提供在 `src/main/resources/db/schema.sql`。

## 注解说明

### @KCScheduled

Spring `@Scheduled` 的增强版，增加管理元数据。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `cron` | String | `""` | Cron 表达式 |
| `zone` | String | `""` | 时区 |
| `cronName` | String | `""` | 管理界面显示的任务名称 |
| `cronDocs` | String | `""` | 任务描述 |
| `isRun` | boolean | `true` | 任务初始是否启用 |
| `dirName` | String | `""` | 目录路径，用于分组（用 `\|` 分隔层级） |

### @ServiceLog

AOP 注解，记录方法执行日志，支持敏感字段自动脱敏。

```java
@ServiceLog(value = "处理订单", sensitiveFields = {"password", "token"})
public void processOrder(OrderRequest request) {
    // 方法的参数和返回值会自动记录
    // "password" 和 "token" 字段会被自动脱敏为 "***"
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | String | `""` | 业务描述 |
| `logParams` | boolean | `true` | 是否记录请求参数 |
| `logResult` | boolean | `true` | 是否记录返回值 |
| `sensitiveFields` | String[] | `{}` | 需要脱敏的字段名 |

## 自定义告警渠道

实现 `NotifyChannel` 接口即可扩展告警渠道：

```java
@Component
public class DingTalkNotifyChannel implements NotifyChannel {

    @Override
    public boolean isEnabled() {
        return true; // 可根据配置动态判断
    }

    @Override
    public void send(String msg) {
        // 发送到钉钉 / 飞书 / 企业微信等
    }
}
```

## 启停审计日志

启用或停用任务时，系统会自动记录**操作人**和**操作原因**：

- 管理界面点击「启用」或「停用」时，会弹出输入框要求填写原因
- 操作人自动取当前登录用户名
- 记录保存在 Redis 的任务执行历史列表中，可在「执行详情」里查看
- 如果通过 API 调用，可传入 `operator` 和 `reason` 参数

```bash
# API 调用示例
POST /kcschedule/isRunTask?taskName=TaskClassmethod&isRun=false&operator=admin&reason=夜间不需要执行
```

## 完整配置参考

```yaml
kcschedule:
  scan-base-package: com.example        # @KCScheduled 任务扫描的基础包
  max-pool: 20                          # 每个任务最大保留执行历史条数
  queue-name: kcschedule:error          # Redis 告警消息队列 Key
  web:
    username: admin                     # Web 管理界面登录用户名
    password: admin                     # Web 管理界面登录密码
  notify:
    channels: email                     # 启用的通知渠道（逗号分隔）
    mail:
      host: smtp.example.com            # SMTP 服务器地址
      port: 465                         # SMTP 服务器端口
      username: sender@example.com      # 发件人邮箱
      password: ***                     # 发件人密码
      recipients: receiver@example.com  # 收件人（逗号分隔）
```

## 架构设计

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Web 管理界面    │────►│ ScheduleController│────►│  ScanService    │
│   (MiniUI)      │     │  /kcschedule/*   │     │  (Redis CRUD)   │
└─────────────────┘     └──────────────────┘     └────────┬────────┘
                                                         │
┌─────────────────┐     ┌──────────────────┐             │
│  NotifyService  │────►│  NotifyChannel   │             ▼
│  (Redis 队列)    │     │  (邮件/自定义)     │     ┌────────────────┐
└─────────────────┘     └──────────────────┘     │     Redis      │
                                                 │  kcs:task:*    │
┌─────────────────┐     ┌──────────────────┐     └────────────────┘
│ @KCScheduled AOP │────►│ScheduledTaskMonitor│
│ (运行时控制)     │     │ (执行状态追踪)     │
└─────────────────┘     └──────────────────┘
```

## 环境要求

- Java 8+
- Spring Boot 2.x 或 3.x
- Redis（用于任务状态存储）

## 许可证

[MIT License](LICENSE)

---

<a id="english-documentation"></a>
# English Documentation

## Why KCSchedule?

Spring's `@Scheduled` annotation makes it easy to create scheduled tasks, but in production you quickly hit these pain points:

- **No runtime control** — you can't enable/disable tasks without restarting the application
- **No visibility** — you can't see task execution status, history, or errors
- **No alerting** — when a task fails, nobody knows until users complain
- **No organization** — as tasks grow, there's no way to group or browse them

Existing solutions like [XXL-JOB](https://github.com/xuxueli/xxl-job) and [PowerJob](https://github.com/PowerJob/PowerJob) are powerful distributed schedulers, but they require deploying a separate scheduling center. For single-application projects or small-to-medium systems, that's overkill.

> There are **very few** lightweight scheduled task management frameworks designed specifically for Spring Boot single-application scenarios. KCSchedule fills this gap — zero deployment, annotation-driven, works out of the box.

## Features

- **Annotation-driven** — replace `@Scheduled` with `@KCScheduled`, everything else is automatic
- **Built-in Web UI** — task dashboard with tree view, execution history, manual trigger
- **Runtime control** — enable/disable tasks via Redis without restarting
- **AOP monitoring** — automatically records execution time, success/failure, error details
- **Multi-channel alerting** — extensible notification system (email out of the box)
- **Task grouping** — organize tasks in hierarchical directories using `dirName`
- **Service logging** — `@ServiceLog` annotation for method-level execution logging with sensitive field masking
- **Zero deployment** — embeds directly in your Spring Boot application, no external services needed

## Comparison

| Feature | KCSchedule | XXL-JOB | PowerJob |
|---------|-----------|---------|----------|
| Deployment | Embedded (JAR) | Separate scheduling center | Separate scheduling center |
| Code changes | Replace annotation | Implement JobHandler | Implement processor |
| Distributed | No (single app) | Yes | Yes |
| Web UI | Built-in | Built-in | Built-in |
| Alerting | Email (extensible) | Email | Email / DingTalk / WeChat |
| Learning curve | Minimal | Medium | Medium |
| Best for | Single-app task management | Distributed task scheduling | Complex workflow orchestration |

## Prerequisites

> **Important: This project is built on Spring Boot** and relies on Spring's `@Scheduled` annotation mechanism.

1. **Must be a Spring Boot project** — The framework scans for `@KCScheduled` annotations during the `ApplicationReadyEvent` lifecycle. Pure Spring Framework projects (non-Boot) would need to adapt the startup flow themselves
2. **Must use annotation-based scheduled tasks** — If you define tasks via XML configuration, `Timer`, direct Quartz integration, etc., KCSchedule cannot manage them. Only tasks defined with `@Scheduled` (or this project's `@KCScheduled`) can be recognized
3. **Redis is required** — Task runtime state and execution history are stored in Redis. This is the only external runtime dependency

> 💡 If you already have tasks using `@Scheduled`, just replace them with `@KCScheduled` to integrate — near-zero migration cost.

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.kcschedule</groupId>
    <artifactId>kcschedule</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configure

```yaml
spring:
  redis:
    host: localhost
    port: 6379

kcschedule:
  scan-base-package: com.yourpackage  # package to scan for @KCScheduled tasks
  max-pool: 20                         # max execution history per task
  web:
    username: admin                    # web UI login username
    password: admin                    # web UI login password
  notify:
    channels: email                    # enable email notifications
    mail:
      host: smtp.example.com
      port: 465
      username: your@email.com
      password: your-password
      recipients: receiver@example.com
  queue-name: kcschedule:error         # Redis queue for error messages
```

### 3. Use @KCScheduled

Replace `@Scheduled` with `@KCScheduled`:

```java
@Component
public class MyTasks {

    @KCScheduled(cron = "0 0/5 * * * ?",
                 cronName = "Data Sync",
                 cronDocs = "Sync data from external system every 5 minutes",
                 dirName = "Data|Sync")
    public void syncData() {
        // your task logic
    }

    @KCScheduled(cron = "0 0 2 * * ?",
                 cronName = "Daily Report",
                 cronDocs = "Generate daily report at 2 AM",
                 dirName = "Report",
                 isRun = false)  // initially disabled
    public void generateReport() {
        // your task logic
    }
}
```

### 4. Access the Web UI

Open `http://localhost:{port}/html/quartzInfo.html` in your browser.

### 5. (Optional) Service Log Persistence

If you want `@ServiceLog` to persist method execution logs to a database:

**Add MyBatis dependency** to your project:
```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
</dependency>
```

**Create the log table**:
```sql
CREATE TABLE kcschedule_service_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    method_name VARCHAR(255),
    description VARCHAR(500),
    success     INT DEFAULT 0,
    duration    BIGINT,
    params      TEXT,
    result      TEXT,
    error       TEXT,
    timestamp   DATETIME,
    INDEX idx_timestamp (timestamp),
    INDEX idx_method_name (method_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

The SQL script is also available at `src/main/resources/db/schema.sql`.

## Annotations

### @KCScheduled

Extension of Spring's `@Scheduled` with management metadata.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `cron` | String | `""` | Cron expression |
| `zone` | String | `""` | Time zone |
| `cronName` | String | `""` | Display name in the management UI |
| `cronDocs` | String | `""` | Task description |
| `isRun` | boolean | `true` | Whether the task is initially enabled |
| `dirName` | String | `""` | Directory path for grouping (use `\|` for hierarchy) |

### @ServiceLog

AOP annotation for logging method execution with automatic sensitive field masking.

```java
@ServiceLog(value = "Process Order", sensitiveFields = {"password", "token"})
public void processOrder(OrderRequest request) {
    // parameters and return value are automatically logged
    // "password" and "token" fields are masked as "***"
}
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `value` | String | `""` | Business description |
| `logParams` | boolean | `true` | Whether to log method parameters |
| `logResult` | boolean | `true` | Whether to log return value |
| `sensitiveFields` | String[] | `{}` | Field names to mask in logs |

## Custom Notification Channel

Implement `NotifyChannel` interface to add custom notification channels:

```java
@Component
public class DingTalkNotifyChannel implements NotifyChannel {

    @Override
    public boolean isEnabled() {
        return true; // or check configuration
    }

    @Override
    public void send(String msg) {
        // send to DingTalk / Slack / WeChat etc.
    }
}
```

## Enable/Disable Audit Log

When enabling or disabling a task, the system automatically records the **operator** and **reason**:

- The web UI prompts for a reason when you click "Enable" or "Disable"
- The operator is automatically taken from the logged-in username
- The record is saved in Redis alongside the task's execution history, visible in the "Execution Details" panel
- When calling via API, pass `operator` and `reason` parameters

```bash
# API example
POST /kcschedule/isRunTask?taskName=TaskClassmethod&isRun=false&operator=admin&reason=Not+needed+at+night
```

## Configuration Reference

```yaml
kcschedule:
  scan-base-package: com.example        # base package for scanning @KCScheduled tasks
  max-pool: 20                          # max execution history records per task
  queue-name: kcschedule:error          # Redis list key for error notifications
  web:
    username: admin                     # web UI basic auth username
    password: admin                     # web UI basic auth password
  notify:
    channels: email                     # comma-separated list of enabled channels
    mail:
      host: smtp.example.com            # SMTP server host
      port: 465                         # SMTP server port
      username: sender@example.com      # SMTP username
      password: ***                     # SMTP password
      recipients: receiver@example.com  # comma-separated recipient list
```

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Web UI        │────►│ ScheduleController│────►│  ScanService    │
│   (MiniUI)      │     │  /kcschedule/*   │     │  (Redis CRUD)   │
└─────────────────┘     └──────────────────┘     └────────┬────────┘
                                                         │
┌─────────────────┐     ┌──────────────────┐             │
│  NotifyService  │────►│  NotifyChannel   │             ▼
│  (Redis Queue)  │     │  (Email/Custom)  │     ┌────────────────┐
└─────────────────┘     └──────────────────┘     │     Redis      │
                                                 │  kcs:task:*    │
┌─────────────────┐     ┌──────────────────┐     └────────────────┘
│ @KCScheduled AOP │────►│ScheduledTaskMonitor│
│ (runtime ctrl)  │     │ (execution track) │
└─────────────────┘     └──────────────────┘
```

## Requirements

- Java 8+
- Spring Boot 2.x or 3.x
- Redis (for task state storage)

## License

[MIT License](LICENSE)
