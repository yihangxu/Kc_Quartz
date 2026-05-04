package com.kcschedule.core.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.kcschedule.core.config.KCScheduleConfig;
import com.kcschedule.core.service.ScanInfoService;
import com.kcschedule.core.service.ScanService;
import com.kcschedule.core.msg.NotifyService;
import com.kcschedule.core.utils.WebCompat;
import com.kcschedule.core.vo.DirScanVo;
import com.kcschedule.core.vo.MethodLogDTO;
import com.kcschedule.core.vo.MiniUIPageInfo;
import com.kcschedule.core.vo.ScheduledInfoVo;
import com.kcschedule.core.vo.TaskSwitchLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping(value = "/kcschedule")
public class ScheduleController {

    @Autowired
    private KCScheduleConfig scheduleConfig;

    @Autowired
    private ScanService scanService;

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private ScanInfoService scanInfoService;

    @Autowired(required = false)
    private com.kcschedule.core.dao.TaskSwitchLogDao taskSwitchLogDao;

    private static final String SESSION_USER_KEY = "kcschedule_user";

    @RequestMapping(value = "/login")
    public boolean login(String userName, String password) {
        if (Objects.equals(userName, scheduleConfig.getUserName())
                && Objects.equals(password, scheduleConfig.getPassword())) {
            WebCompat.setSessionAttribute(SESSION_USER_KEY, userName);
            return true;
        }
        return false;
    }

    @RequestMapping(value = "/getCurrentUser")
    public String getCurrentUser() {
        Object user = WebCompat.getSessionAttribute(SESSION_USER_KEY);
        return user != null ? user.toString() : "";
    }

    @RequestMapping(value = "/getScan")
    public LinkedList<ScheduledInfoVo> getScan(String scheduledName, String functionName, String type, String isRun) {
        return scanService.getScanTasks(scheduledName, functionName, type, isRun);
    }

    @RequestMapping(value = "/getDirScan")
    public DirScanVo getDirScan(String scheduledName, String functionName, String type, String isRun) {
        DirScanVo scanVo = new DirScanVo();
        scanVo.setDisplayName("All");
        scanVo.setDirName("");
        Map<String, DirScanVo> scanMap = new LinkedHashMap<>();
        scanMap.put("", scanVo);
        List<ScheduledInfoVo> scheduledList = scanService.getScanTasks(scheduledName, functionName, type, isRun);
        for (ScheduledInfoVo item : scheduledList) {
            if (StrUtil.isNotEmpty(item.getDirName())) {
                String[] dirNames = item.getDirName().split("\\|");
                String currentName = "";
                String parentName = "";
                for (String name : dirNames) {
                    currentName = StrUtil.isNotEmpty(currentName) ? currentName + "|" + name : name;
                    if (scanMap.containsKey(currentName)) {
                        scanMap.get(currentName).getScheduledList().add(item);
                    } else {
                        DirScanVo child = new DirScanVo();
                        child.setDisplayName(name);
                        child.setDirName(currentName);
                        child.getScheduledList().add(item);
                        scanMap.put(currentName, child);
                        scanMap.get(parentName).getChildList().add(child);
                    }
                    parentName = currentName;
                }
            }
        }
        scanVo.setScheduledList(scheduledList);
        return scanVo;
    }

    @RequestMapping(value = "/taskName")
    public LinkedList<ScheduledInfoVo> getScanInfo(String taskName) {
        if (taskName == null) return null;
        LinkedList<ScheduledInfoVo> runList = scanService.getScheduledInfoVo(taskName);
        if (CollUtil.isNotEmpty(runList)) {
            runList.sort((o1, o2) -> {
                String date1 = o1.getRunDate();
                String date2 = o2.getRunDate();
                if (date1 == null && date2 == null) return 0;
                if (date1 == null) return 1;
                if (date2 == null) return -1;
                return date2.compareTo(date1);
            });
        }
        return runList;
    }

    @RequestMapping(value = "/isRunTask")
    public String isRunTask(String taskName, String isRun) {
        if (taskName == null) return null;
        return scanService.isRunTaskValue(taskName, isRun);
    }

    @RequestMapping(value = "/runTask")
    public String runTask(String taskName) {
        if (!scanService.isTask(taskName)) {
            return "no task name";
        }
        return scanService.runTask(taskName);
    }

    @RequestMapping(value = "/scanLogPage", method = RequestMethod.POST)
    public MiniUIPageInfo<MethodLogDTO> scanLogPage(MethodLogDTO methodLogDTO) {
        return notifyService.scanLogPage(methodLogDTO);
    }

    @RequestMapping(value = "/refreshTasks")
    public String refreshTasks() {
        return scanInfoService.refreshScheduledTasks();
    }

    @RequestMapping(value = "/isRunTaskWithReason")
    public String isRunTaskWithReason(String taskName, String scheduledName, String isRun, String reason, String operator) {
        if (taskName == null) {
            return "task name is required";
        }
        if (reason == null || reason.trim().isEmpty()) {
            return "reason is required";
        }
        return scanService.isRunTaskWithReason(taskName, scheduledName, isRun, reason, operator);
    }

    @RequestMapping(value = "/getLatestSwitchLog")
    public TaskSwitchLog getLatestSwitchLog(String taskName) {
        if (taskName == null) return null;
        return scanService.getLatestSwitchLog(taskName);
    }

    @RequestMapping(value = "/querySwitchLogPage")
    public MiniUIPageInfo<TaskSwitchLog> querySwitchLogPage(TaskSwitchLog query) {
        if (taskSwitchLogDao == null) {
            MiniUIPageInfo<TaskSwitchLog> empty = new MiniUIPageInfo<>();
            empty.setData(new java.util.ArrayList<>());
            empty.setTotal(0);
            return empty;
        }
        int pageNum = (query.getPageIndex() == null || query.getPageIndex() < 0) ? 0 : query.getPageIndex();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        int offset = pageNum * pageSize;
        query.setOffset(offset);

        List<TaskSwitchLog> dataList = taskSwitchLogDao.queryPage(query);
        int total = taskSwitchLogDao.count(query);

        MiniUIPageInfo<TaskSwitchLog> pageInfo = new MiniUIPageInfo<>();
        pageInfo.setData(dataList);
        pageInfo.setTotal(total);
        return pageInfo;
    }
}
