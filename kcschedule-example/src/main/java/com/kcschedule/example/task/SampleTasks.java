package com.kcschedule.example.task;

import com.kcschedule.core.annotation.KCScheduled;
import com.kcschedule.core.annotation.ServiceLog;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SampleTasks {

    /**
     * Data sync task - runs every 5 minutes, grouped under "Data|Sync"
     */
    @KCScheduled(cron = "0 0/5 * * * ?",
                 cronName = "Data Sync",
                 cronDocs = "Sync data from external system every 5 minutes",
                 dirName = "Data|Sync")
    public void syncData() {
        System.out.println("[Data Sync] Running at " + LocalDateTime.now());
    }

    /**
     * Daily report - runs at 2 AM, initially disabled
     */
    @KCScheduled(cron = "0 0 2 * * ?",
                 cronName = "Daily Report",
                 cronDocs = "Generate daily report at 2 AM",
                 dirName = "Report",
                 isRun = false)
    public void generateReport() {
        System.out.println("[Daily Report] Generating report at " + LocalDateTime.now());
    }

    /**
     * Health check - runs every 30 seconds, grouped under "Monitor"
     */
    @KCScheduled(cron = "0/30 * * * * ?",
                 cronName = "Health Check",
                 cronDocs = "Check service health status every 30 seconds",
                 dirName = "Monitor")
    public void healthCheck() {
        System.out.println("[Health Check] OK at " + LocalDateTime.now());
    }

    /**
     * Demo for @ServiceLog with sensitive field masking
     */
    @ServiceLog(value = "Process Order", sensitiveFields = {"password", "creditCard"})
    public String processOrder(String orderId, String password, String creditCard) {
        // password and creditCard will be masked as *** in logs
        return "Order " + orderId + " processed";
    }
}
