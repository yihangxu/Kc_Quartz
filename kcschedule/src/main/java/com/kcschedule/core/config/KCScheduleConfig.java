package com.kcschedule.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KCScheduleConfig {

    @Value("${kcschedule.max-pool:20}")
    private Integer maxPool;

    @Value("${kcschedule.web.username:admin}")
    private String userName;

    @Value("${kcschedule.web.password:admin}")
    private String password;

    @Value("${kcschedule.notify.mail.host:}")
    private String mailHost;

    @Value("${kcschedule.notify.mail.port:465}")
    private String mailPort;

    @Value("${kcschedule.notify.mail.username:}")
    private String mailUsername;

    @Value("${kcschedule.notify.mail.password:}")
    private String mailPassword;

    @Value("${kcschedule.notify.mail.recipients:}")
    private String mailRecipients;

    @Value("${kcschedule.notify.channels:}")
    private List<String> notifyChannels;

    @Value("${kcschedule.queue-name:kcschedule:error}")
    private String queueName;

    @Value("${kcschedule.scan-base-package:}")
    private String scanBasePackage;

    public Integer getMaxPool() { return maxPool; }
    public void setMaxPool(Integer maxPool) { this.maxPool = maxPool; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMailHost() { return mailHost; }
    public void setMailHost(String mailHost) { this.mailHost = mailHost; }

    public String getMailPort() { return mailPort; }
    public void setMailPort(String mailPort) { this.mailPort = mailPort; }

    public String getMailUsername() { return mailUsername; }
    public void setMailUsername(String mailUsername) { this.mailUsername = mailUsername; }

    public String getMailPassword() { return mailPassword; }
    public void setMailPassword(String mailPassword) { this.mailPassword = mailPassword; }

    public String getMailRecipients() { return mailRecipients; }
    public void setMailRecipients(String mailRecipients) { this.mailRecipients = mailRecipients; }

    public List<String> getNotifyChannels() { return notifyChannels; }
    public void setNotifyChannels(List<String> notifyChannels) { this.notifyChannels = notifyChannels; }

    public String getQueueName() { return queueName; }
    public void setQueueName(String queueName) { this.queueName = queueName; }

    public String getScanBasePackage() { return scanBasePackage; }
    public void setScanBasePackage(String scanBasePackage) { this.scanBasePackage = scanBasePackage; }
}
