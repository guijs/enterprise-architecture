package com.company.crypto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段加密标记。仅对高敏感字段加密，避免全表加密拖垮性能与索引。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldEncrypt {

    /** 是否同时写入哈希列，供等值查询（如按手机号登录）。 */
    boolean hashQuery() default false;
}
