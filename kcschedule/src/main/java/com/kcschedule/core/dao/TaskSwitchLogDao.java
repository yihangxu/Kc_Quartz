package com.kcschedule.core.dao;

import com.kcschedule.core.vo.TaskSwitchLog;

import java.util.List;

public interface TaskSwitchLogDao {

    void save(TaskSwitchLog log);

    List<TaskSwitchLog> queryPage(TaskSwitchLog query);

    int count(TaskSwitchLog query);

    TaskSwitchLog findLatestByTaskName(String taskName);
}
