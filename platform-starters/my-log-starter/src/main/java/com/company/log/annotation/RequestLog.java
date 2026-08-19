package com.company.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 接口日志，可标在类或方法。优先级：方法 > 类 > yaml 全局。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestLog {

    LogSwitch enabled() default LogSwitch.DEFAULT;

    LogSwitch request() default LogSwitch.DEFAULT;

    LogSwitch response() default LogSwitch.DEFAULT;

    String[] ignoreParams() default {};
}
