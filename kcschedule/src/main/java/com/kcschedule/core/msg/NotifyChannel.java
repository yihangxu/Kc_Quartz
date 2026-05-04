package com.kcschedule.core.msg;

public interface NotifyChannel {
    void send(String msg);
    boolean isEnabled();
}
