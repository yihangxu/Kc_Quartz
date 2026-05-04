package com.kcschedule.core.vo;

import java.time.LocalDateTime;

public class MethodLogDTO {
    private Integer id;
    private String methodName;
    private String description;
    private Integer success;
    private long duration;
    private String params;
    private String result;
    private String error;
    private LocalDateTime timestamp;
    private Integer pageIndex;
    private Integer pageSize;
    private Integer offset;
    private String endTime;
    private String startTime;

    public MethodLogDTO() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String methodName;
        private String description;
        private Integer success;
        private long duration;
        private String params;
        private String result;
        private String error;
        private LocalDateTime timestamp;

        public Builder methodName(String methodName) { this.methodName = methodName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder success(Integer success) { this.success = success; return this; }
        public Builder duration(long duration) { this.duration = duration; return this; }
        public Builder params(String params) { this.params = params; return this; }
        public Builder result(String result) { this.result = result; return this; }
        public Builder error(String error) { this.error = error; return this; }
        public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public MethodLogDTO build() { return new MethodLogDTO(this); }
    }

    private MethodLogDTO(Builder builder) {
        this.methodName = builder.methodName;
        this.description = builder.description;
        this.success = builder.success;
        this.duration = builder.duration;
        this.params = builder.params;
        this.result = builder.result;
        this.error = builder.error;
        this.timestamp = builder.timestamp;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Integer getPageIndex() { return pageIndex; }
    public void setPageIndex(Integer pageIndex) { this.pageIndex = pageIndex; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public Integer getOffset() { return offset; }
    public void setOffset(Integer offset) { this.offset = offset; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public Integer getSuccess() { return success; }
    public void setSuccess(Integer success) { this.success = success; }
}
