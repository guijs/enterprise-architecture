package com.company.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解（Redis 滑动窗口）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** SpEL 指定 key，不填则默认 IP + 接口路径。 */
    String key() default "";

    /** 时间窗口内最大请求次数。 */
    long limit() default 100;

    /** 时间窗口大小。 */
    long window() default 1;

    TimeUnit timeUnit() default TimeUnit.MINUTES;

    String message() default "请求过于频繁，请稍后再试";
}
