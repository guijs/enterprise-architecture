package com.company.idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口幂等注解。两态语义：PENDING（处理中）/ DONE（已完成）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** SpEL 指定 key，不填则取请求 Header 中的 Idempotent-Token。 */
    String key() default "";

    /** 处理中占位的 TTL（防止实例宕机后 key 永久占用）。 */
    long expire() default 60;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /** 是否缓存并返回首次执行结果（true 时命中 DONE 直接返回，天然幂等）。 */
    boolean cacheResult() default false;

    /** 结果缓存 TTL（秒），cacheResult=true 时生效。 */
    long resultExpire() default 300;

    String message() default "请勿重复提交";
}
