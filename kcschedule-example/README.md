# KCSchedule Example

This is a minimal example project demonstrating how to use KCSchedule.

## Prerequisites

1. **Redis** running on `localhost:6379`
2. **MySQL** running on `localhost:3306` (optional, for log persistence)

## Quick Start

### 1. Build KCSchedule

```bash
cd ../kcschedule
mvn clean install
```

### 2. Initialize Database (optional)

```sql
CREATE DATABASE IF NOT EXISTS kcschedule_example DEFAULT CHARSET utf8mb4;
```

Then execute the SQL script: `src/main/resources/db/schema.sql`

### 3. Run

```bash
mvn spring-boot:run
```

### 4. Open Management UI

http://localhost:8080/kcschedule/html/quartzInfo.html

Login: `admin` / `admin`

## What You'll See

- **3 sample tasks** in the tree view: Data|Sync, Report, Monitor|Health Check
- **Health Check** runs every 30 seconds — watch its execution history update
- **Daily Report** is initially disabled — try enabling it
- Click enable/disable to see the audit log prompt
- Click "Manual Trigger" to run a task immediately

## Key Points

- `@KCScheduled` replaces `@Scheduled`, everything else is automatic
- `dirName` organizes tasks in a tree structure (use `|` for hierarchy)
- `isRun = false` starts a task in disabled state
- `scan-base-package` tells KCSchedule where to find your tasks
- Redis stores task state and execution history
- Database tables persist audit logs and service logs permanently
