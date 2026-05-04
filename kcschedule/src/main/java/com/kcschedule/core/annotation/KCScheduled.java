package com.kcschedule.core.annotation;

import org.springframework.scheduling.annotation.Scheduled;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scheduled
public @interface KCScheduled {

    String cron() default "";

    String zone() default "";

    /** Display name of the task shown in the management UI */
    String cronName() default "";

    /** Description of the task shown in the management UI */
    String cronDocs() default "";

    /** Whether the task is initially enabled */
    boolean isRun() default true;

    /** Directory path for grouping tasks, use "|" for hierarchy (e.g. "Data|Sync") */
    String dirName() default "";
}
