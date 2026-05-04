package com.kcschedule.core.dao;

import com.kcschedule.core.vo.MethodLogDTO;

import java.util.List;

public interface LogPersistDao {
    void save(MethodLogDTO log);

    List<MethodLogDTO> queryLogPage(MethodLogDTO query);

    int countLog(MethodLogDTO query);
}
