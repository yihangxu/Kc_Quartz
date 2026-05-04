package com.kcschedule.core.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceLog {
    String value() default "";
    boolean logParams() default true;
    boolean logResult() default true;
    String[] sensitiveFields() default {};
}
