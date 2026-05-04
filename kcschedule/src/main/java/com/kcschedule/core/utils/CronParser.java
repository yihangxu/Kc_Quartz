package com.kcschedule.core.utils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class CronParser {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getNextExecution(String cron, Date date) {
        try {
            Class<?> clazz = Class.forName("org.springframework.scheduling.support.CronExpression");
            Object expression = clazz.getMethod("parse", String.class).invoke(null, cron);
            Object next = clazz.getMethod("next", java.time.temporal.Temporal.class)
                    .invoke(expression, date.toInstant().atZone(ZoneId.systemDefault()));
            return FORMATTER.format((java.time.temporal.TemporalAccessor) next);
        } catch (ReflectiveOperationException e) {
            try {
                Class<?> clazz = Class.forName("org.springframework.scheduling.support.CronSequenceGenerator");
                Object generator = clazz.getConstructor(String.class).newInstance(cron);
                Date next = (Date) clazz.getMethod("next", Date.class).invoke(generator, date);
                return FORMATTER.format(next.toInstant().atZone(ZoneId.systemDefault()));
            } catch (ReflectiveOperationException ex) {
                throw new UnsupportedOperationException("Failed to parse cron expression: " + cron, ex);
            }
        }
    }
}
