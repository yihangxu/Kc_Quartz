package com.kcschedule.core.vo;

import java.util.ArrayList;
import java.util.List;

public class DirScanVo {
    private String dirName;
    private String displayName;
    private List<DirScanVo> childList = new ArrayList<>();
    private List<ScheduledInfoVo> scheduledList = new ArrayList<>();

    public String getDirName() { return dirName; }
    public void setDirName(String dirName) { this.dirName = dirName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public List<DirScanVo> getChildList() { return childList; }
    public void setChildList(List<DirScanVo> childList) { this.childList = childList; }

    public List<ScheduledInfoVo> getScheduledList() { return scheduledList; }
    public void setScheduledList(List<ScheduledInfoVo> scheduledList) { this.scheduledList = scheduledList; }
}
