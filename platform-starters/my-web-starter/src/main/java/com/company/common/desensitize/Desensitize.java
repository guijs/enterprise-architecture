package com.company.common.desensitize;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段脱敏注解：标注在 VO 字段上，Jackson 序列化时自动打码。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DesensitizeSerializer.class)
public @interface Desensitize {

    DesensitizeType type();

    /** CUSTOM 时生效：保留前 N 位。 */
    int prefixKeep() default 0;

    /** CUSTOM 时生效：保留后 N 位。 */
    int suffixKeep() default 0;
}
