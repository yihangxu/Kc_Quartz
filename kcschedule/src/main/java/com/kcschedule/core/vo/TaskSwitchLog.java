package com.kcschedule.core.vo;

import java.time.LocalDateTime;

public class TaskSwitchLog {
    private Long id;
    private String taskName;
    private String scheduledName;
    private String operation;
    private String reason;
    private String operator;
    private LocalDateTime operateTime;

    private Integer pageIndex;
    private Integer pageSize;
    private Integer offset;

    public TaskSwitchLog() {}

    public TaskSwitchLog(String taskName, String scheduledName, String operation, String reason, String operator) {
        this.taskName = taskName;
        this.scheduledName = scheduledName;
        this.operation = operation;
        this.reason = reason;
        this.operator = operator;
        this.operateTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getScheduledName() { return scheduledName; }
    public void setScheduledName(String scheduledName) { this.scheduledName = scheduledName; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public LocalDateTime getOperateTime() { return operateTime; }
    public void setOperateTime(LocalDateTime operateTime) { this.operateTime = operateTime; }

    public Integer getPageIndex() { return pageIndex; }
    public void setPageIndex(Integer pageIndex) { this.pageIndex = pageIndex; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public Integer getOffset() { return offset; }
    public void setOffset(Integer offset) { this.offset = offset; }
}
