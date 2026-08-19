package com.company.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计日志：记录「谁在何时对什么做了什么」，异步落库，与请求日志分离存储。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 业务模块，如「订单」「用户」。 */
    String module();

    /** 操作类型：CREATE / UPDATE / DELETE / EXPORT / LOGIN ... */
    String type();

    /** 操作描述，支持 SpEL，如「创建订单：#{#req.orderNo}」。 */
    String content() default "";

    boolean saveParams() default true;

    boolean saveResult() default false;
}
