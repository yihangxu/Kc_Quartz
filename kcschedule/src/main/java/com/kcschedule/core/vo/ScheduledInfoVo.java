package com.kcschedule.core.vo;

public class ScheduledInfoVo {
    private String taskName;
    private String scheduledName;
    private String cron;
    private String describe;
    private String nextDate;
    private String runDate;
    private Long runTime;
    private String errorMsg;
    private String flag;
    private String className;
    private String functionName;
    private boolean isRun;
    private String dirName;
    private String operator;
    private String operateReason;

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getScheduledName() { return scheduledName; }
    public void setScheduledName(String scheduledName) { this.scheduledName = scheduledName; }

    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }

    public String getDescribe() { return describe; }
    public void setDescribe(String describe) { this.describe = describe; }

    public String getNextDate() { return nextDate; }
    public void setNextDate(String nextDate) { this.nextDate = nextDate; }

    public String getRunDate() { return runDate; }
    public void setRunDate(String runDate) { this.runDate = runDate; }

    public Long getRunTime() { return runTime; }
    public void setRunTime(Long runTime) { this.runTime = runTime; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }

    public String getFunctionName() { return functionName; }
    public void setFunctionName(String functionName) { this.functionName = functionName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public boolean getIsRun() { return isRun; }
    public void setIsRun(boolean run) { this.isRun = run; }

    public String getDirName() { return dirName; }
    public void setDirName(String dirName) { this.dirName = dirName; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getOperateReason() { return operateReason; }
    public void setOperateReason(String operateReason) { this.operateReason = operateReason; }
}
