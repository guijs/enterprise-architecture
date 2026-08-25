package com.company.lock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /** SpEL 表达式，支持多参数拼接，如 {"#orderId", "#userId"}。 */
    String[] keys() default {};

    /** key 前缀，不填则默认 类名:方法名。 */
    String prefix() default "";

    /** 等待获取锁超时（默认 3s）。 */
    long waitTime() default 3;

    /** 持锁时间（-1 = watchdog 自动续期）。 */
    long leaseTime() default -1;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    boolean fair() default false;

    String message() default "操作频繁，请稍后重试";

    /** 自定义异常类型（需有 String 构造器）。 */
    Class<? extends RuntimeException> exception() default LockException.class;
}
