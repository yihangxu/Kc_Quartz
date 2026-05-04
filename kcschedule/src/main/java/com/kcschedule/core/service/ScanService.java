package com.kcschedule.core.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.kcschedule.core.config.KCScheduleConfig;
import com.kcschedule.core.aop.ScheduledTaskMonitor;
import com.kcschedule.core.vo.ScheduledInfoVo;
import com.kcschedule.core.vo.TaskSwitchLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ScanService {

    private static final Logger logger = LoggerFactory.getLogger(ScanService.class);
    private static final String KEY_PREFIX = "kcs:task:";
    public static final String RUN_KEY_PREFIX = "kcs:task:run:";

    @Autowired
    private KCScheduleConfig scheduleConfig;

    @Lazy
    @Autowired
    private ScanInfoService scanInfoService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private com.kcschedule.core.dao.TaskSwitchLogDao taskSwitchLogDao;

    public boolean isTask(String taskName) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + taskName));
    }

    public boolean isRunTask(String taskName) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RUN_KEY_PREFIX + taskName));
    }

    public String isRunTaskValue(String taskName, String isRun) {
        redisTemplate.opsForValue().set(RUN_KEY_PREFIX + taskName, isRun);
        return "success";
    }

    public String isRunTaskWithReason(String taskName, String scheduledName, String isRun, String reason, String operator) {
        redisTemplate.opsForValue().set(RUN_KEY_PREFIX + taskName, isRun);

        if (taskSwitchLogDao != null) {
            String operation = "true".equals(isRun) ? "启用" : "停用";
            TaskSwitchLog log = new TaskSwitchLog(taskName, scheduledName, operation, reason, operator);
            taskSwitchLogDao.save(log);
        }

        logger.info("Task [{}] {}, operator: {}, reason: {}", taskName, "true".equals(isRun) ? "enabled" : "disabled", operator, reason);
        return "success";
    }

    public TaskSwitchLog getLatestSwitchLog(String taskName) {
        if (taskSwitchLogDao == null) return null;
        return taskSwitchLogDao.findLatestByTaskName(taskName);
    }

    public LinkedList<ScheduledInfoVo> getScheduledInfoVo(String taskName) {
        String key = KEY_PREFIX + taskName;
        return Optional.ofNullable(getRedisValue(key))
                .map(json -> new LinkedList<>(JSONUtil.toList(json, ScheduledInfoVo.class)))
                .orElse(new LinkedList<>());
    }

    public String getRedisValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public synchronized LinkedList<ScheduledInfoVo> addTaskToList(LinkedList<ScheduledInfoVo> taskList, ScheduledInfoVo taskInfo) {
        if (taskList == null) {
            taskList = new LinkedList<>();
        }
        if (taskList.size() >= scheduleConfig.getMaxPool()) {
            taskList.poll();
        }
        taskList.add(taskInfo);
        return taskList;
    }

    public LinkedList<ScheduledInfoVo> getScanTasks(String scheduledName, String functionName, String type, String isRun) {
        Map<String, LinkedList<ScheduledInfoVo>> scheduledInfoVoMap = new HashMap<>();
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null) {
            return new LinkedList<>();
        }
        keys.forEach(key -> {
            if (key.startsWith(RUN_KEY_PREFIX)) return;
            String redisValue = getRedisValue(key);
            if (redisValue != null) {
                List<ScheduledInfoVo> infoVoList;
                try {
                    infoVoList = JSONUtil.toList(redisValue, ScheduledInfoVo.class);
                } catch (Exception e) {
                    logger.warn("Invalid JSON for task key {}, skipping: {}", key, e.getMessage());
                    return;
                }
                String taskKey = key.replace(KEY_PREFIX, "");
                String runTask = getRedisValue(RUN_KEY_PREFIX + taskKey);
                if (StrUtil.isNotEmpty(runTask)) {
                    infoVoList = infoVoList.stream()
                            .peek(vo -> vo.setIsRun(Boolean.parseBoolean(runTask)))
                            .collect(Collectors.toList());
                }
                scheduledInfoVoMap.put(taskKey, new LinkedList<>(infoVoList));
            }
        });
        List<ScheduledInfoVo> result = scheduledInfoVoMap.entrySet().stream().map(entry -> {
            LinkedList<ScheduledInfoVo> valueList = entry.getValue();
            if (valueList.isEmpty()) return null;
            ScheduledInfoVo scheduledInfoVo = valueList.getLast();
            scheduledInfoVo.setTaskName(entry.getKey());
            return scheduledInfoVo;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return new LinkedList<>(filterScanTasks(scheduledName, functionName, type, result, isRun));
    }

    private List<ScheduledInfoVo> filterScanTasks(String scheduledName, String functionName, String type, List<ScheduledInfoVo> tasks, String isRun) {
        return tasks.stream()
                .filter(vo -> type == null || type.isEmpty() || type.equals(vo.getFlag()))
                .filter(vo -> isRun == null || isRun.isEmpty() || isRun.equals(Boolean.toString(vo.getIsRun())))
                .filter(vo -> scheduledName == null || scheduledName.isEmpty()
                        || (vo.getScheduledName() != null && vo.getScheduledName().contains(scheduledName)))
                .filter(vo -> StrUtil.isEmpty(functionName) || vo.getFunctionName().contains(functionName))
                .collect(Collectors.toList());
    }

    public void setScheduledInfoVo(String taskName, LinkedList<ScheduledInfoVo> scheduledInfoVo) {
        redisTemplate.opsForValue().set(KEY_PREFIX + taskName, JSONUtil.toJsonStr(scheduledInfoVo));
    }

    public String runTask(String taskName) {
        String key = KEY_PREFIX + taskName;
        String redisValue = getRedisValue(key);
        if (redisValue == null) {
            logger.warn("Task {} not found or expired", taskName);
            return "not_found";
        }

        List<ScheduledInfoVo> scheduledInfoVos = JSONUtil.toList(redisValue, ScheduledInfoVo.class);
        if (scheduledInfoVos == null || scheduledInfoVos.isEmpty()) {
            logger.warn("Task {} list is empty", taskName);
            return "empty";
        }

        ScheduledInfoVo scheduledInfoVo = scheduledInfoVos.get(scheduledInfoVos.size() - 1);
        String className = scheduledInfoVo.getClassName();
        String functionName = scheduledInfoVo.getFunctionName();

        try {
            ScheduledTaskMonitor.MANUAL_TRIGGER.set(true);
            ApplicationContext context = scanInfoService.getApplicationContext();
            Object bean = context.getBean(Class.forName(className));
            Method method = bean.getClass().getMethod(functionName);
            method.invoke(bean);
            return "success";
        } catch (ClassNotFoundException e) {
            logger.error("Class not found: {}", e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            logger.error("Method not found: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Method execution error: {}", e.getMessage(), e);
        } finally {
            ScheduledTaskMonitor.MANUAL_TRIGGER.remove();
        }
        return "error";
    }
}
