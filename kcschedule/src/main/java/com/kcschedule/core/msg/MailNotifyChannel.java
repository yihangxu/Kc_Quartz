package com.kcschedule.core.msg;

import com.kcschedule.core.config.KCScheduleConfig;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.Properties;

public class MailNotifyChannel implements NotifyChannel, InitializingBean {

    @Autowired
    private KCScheduleConfig scheduleConfig;

    private Object mailSession;
    private String mailPackage;
    private Class<?> messageClass;
    private Class<?> internetAddressClass;

    @Override
    public void afterPropertiesSet() {
        String host = scheduleConfig.getMailHost();
        if (host == null || host.trim().isEmpty()) return;

        mailPackage = detectMailPackage();
        if (mailPackage == null) return;

        try {
            Class<?> sessionClass = Class.forName(mailPackage + ".Session");
            this.messageClass = Class.forName(mailPackage + ".internet.MimeMessage");
            this.internetAddressClass = Class.forName(mailPackage + ".internet.InternetAddress");

            Properties prop = new Properties();
            prop.put("mail.smtp.host", host);
            prop.put("mail.smtp.port", scheduleConfig.getMailPort());
            prop.put("mail.smtp.auth", "true");
            prop.put("mail.smtp.ssl.enable", "true");

            Class<?> authenticatorClass = Class.forName(mailPackage + ".Authenticator");
            Class<?> passwordAuthClass = Class.forName(mailPackage + ".PasswordAuthentication");

            Object authenticator = java.lang.reflect.Proxy.newProxyInstance(
                    authenticatorClass.getClassLoader(),
                    new Class<?>[]{authenticatorClass},
                    (proxy, method, args) -> {
                        if ("getPasswordAuthentication".equals(method.getName())) {
                            return passwordAuthClass.getConstructor(String.class, String.class)
                                    .newInstance(scheduleConfig.getMailUsername(), scheduleConfig.getMailPassword());
                        }
                        return null;
                    });

            Method getInstance = sessionClass.getMethod("getInstance", Properties.class, authenticatorClass);
            this.mailSession = getInstance.invoke(null, prop, authenticator);
        } catch (ClassNotFoundException e) {
            // Mail API not available
        } catch (Exception e) {
            // Mail initialization failed
        }
    }

    private String detectMailPackage() {
        try {
            Class.forName("jakarta.mail.Session");
            return "jakarta.mail";
        } catch (ClassNotFoundException ignored) {}
        try {
            Class.forName("javax.mail.Session");
            return "javax.mail";
        } catch (ClassNotFoundException ignored) {}
        return null;
    }

    @Override
    public boolean isEnabled() {
        return scheduleConfig.getNotifyChannels() != null
                && scheduleConfig.getNotifyChannels().contains("email")
                && scheduleConfig.getMailHost() != null
                && !scheduleConfig.getMailHost().trim().isEmpty();
    }

    @Override
    public void send(String msg) {
        if (mailSession == null) return;
        try {
            Object message = messageClass.getConstructor(
                    Class.forName(mailPackage + ".Session")).newInstance(mailSession);

            Method setFrom = messageClass.getMethod("setFrom", internetAddressClass);
            Object fromAddr = internetAddressClass.getConstructor(String.class)
                    .newInstance(scheduleConfig.getMailUsername());
            setFrom.invoke(message, fromAddr);

            Class<?> recipientTypeClass = Class.forName(mailPackage + ".Message$RecipientType");
            Object toType = recipientTypeClass.getField("TO").get(null);
            Method setRecipients = messageClass.getMethod("setRecipients", recipientTypeClass,
                    java.lang.reflect.Array.newInstance(internetAddressClass, 0).getClass());
            Method parse = internetAddressClass.getMethod("parse", String.class);
            Object addresses = parse.invoke(null, scheduleConfig.getMailRecipients());
            setRecipients.invoke(message, toType, addresses);

            messageClass.getMethod("setSubject", String.class).invoke(message, "[KCSchedule] Task Error Alert");
            messageClass.getMethod("setText", String.class).invoke(message, msg);

            Class<?> transportClass = Class.forName(mailPackage + ".Transport");
            transportClass.getMethod("send", messageClass).invoke(null, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
