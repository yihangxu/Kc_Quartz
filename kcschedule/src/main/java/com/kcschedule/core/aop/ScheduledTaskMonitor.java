package com.kcschedule.core.aop;

import cn.hutool.core.date.DateUtil;
import com.kcschedule.core.service.ScanService;
import com.kcschedule.core.msg.NotifyService;
import com.kcschedule.core.utils.CronParser;
import com.kcschedule.core.vo.ScheduledInfoVo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

@Aspect
@Component
public class ScheduledTaskMonitor {

    public static final ThreadLocal<Boolean> MANUAL_TRIGGER = new ThreadLocal<>();

    @Autowired
    private ScanService scanService;
    @Autowired
    private NotifyService notifyService;

    @Around("@annotation(com.kcschedule.core.annotation.KCScheduled)")
    public Object monitorScheduledTasks(ProceedingJoinPoint joinPoint) throws Throwable {
        ScheduledInfoVo taskInfo = new ScheduledInfoVo();
        LocalDateTime startTime = LocalDateTime.now();
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
        String taskKey = generateTaskKey(joinPoint);
        Object result = null;
        LinkedList<ScheduledInfoVo> scheduledInfoVo = scanService.getScheduledInfoVo(taskKey);
        LinkedList<ScheduledInfoVo> taskInfoList = new LinkedList<>(scheduledInfoVo);
        if (scanService.isTask(taskKey)) {
            if (!taskInfoList.isEmpty()) {
                updateTaskInfo(taskInfo, taskInfoList.getLast());
            }
        }
        boolean isManualTrigger = Boolean.TRUE.equals(MANUAL_TRIGGER.get());
        try {
            MANUAL_TRIGGER.remove();
            boolean isRun = isManualTrigger || shouldRunTask(taskKey, taskInfoList);
            if (isRun) {
                result = joinPoint.proceed();
            }
        } catch (Throwable e) {
            String fullStackTrace = getFullStackTrace(e);
            taskInfo.setErrorMsg(fullStackTrace);
            exceptionOccurred.set(true);
            String functionName = taskInfoList.isEmpty() ? "unknown" : taskInfoList.get(0).getFunctionName();
            String errorMsg = "Scheduled task error, task: " + taskKey
                    + ", error: " + fullStackTrace
                    + ", method: " + functionName
                    + ", time: " + DateUtil.now();
            notifyService.sendErrorMsg(errorMsg);
            throw e;
        } finally {
            boolean isRun = isManualTrigger || shouldRunTask(taskKey, taskInfoList);
            updateTaskRuntimeInfo(startTime, taskInfo, exceptionOccurred.get(), isRun);
            LinkedList<ScheduledInfoVo> updatedTaskInfoList = scanService.addTaskToList(taskInfoList, taskInfo);
            scanService.setScheduledInfoVo(taskKey, updatedTaskInfoList);
        }

        return result;
    }

    private boolean shouldRunTask(String taskKey, LinkedList<ScheduledInfoVo> taskInfoList) {
        String isRunVal = scanService.getRedisValue(ScanService.RUN_KEY_PREFIX + taskKey);
        if (isRunVal != null) {
            return Boolean.parseBoolean(isRunVal);
        }
        if (!taskInfoList.isEmpty()) {
            return taskInfoList.get(0).getIsRun();
        }
        return true;
    }

    private static String getFullStackTrace(Throwable throwable) {
        return throwable.getClass().getName() + ": " + throwable.getMessage();
    }

    private void updateTaskInfo(ScheduledInfoVo target, ScheduledInfoVo source) {
        BeanUtils.copyProperties(source, target);
        target.setNextDate(CronParser.getNextExecution(source.getCron(), new Date()));
        target.setRunDate(DateUtil.now());
    }

    private void updateTaskRuntimeInfo(LocalDateTime startTime, ScheduledInfoVo taskInfo, boolean exceptionOccurred, boolean isRun) {
        LocalDateTime endTime = LocalDateTime.now();
        Duration runtime = Duration.between(startTime, endTime);
        taskInfo.setRunTime(runtime.toMillis());
        taskInfo.setFlag(exceptionOccurred ? "failure" : "success");
        if (!exceptionOccurred) {
            taskInfo.setErrorMsg("");
        }
        if (!isRun) {
            taskInfo.setFlag("disabled");
        }
        taskInfo.setRunDate(DateUtil.now());
    }

    private String generateTaskKey(ProceedingJoinPoint joinPoint) {
        Class<?> declaringType = joinPoint.getSignature().getDeclaringType();
        String objectName = declaringType.getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        return objectName + methodName;
    }
}
