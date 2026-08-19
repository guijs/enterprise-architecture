package com.company.common.enums;

/**
 * 基础枚举约定：前后端、DB 统一用 code，展示用 desc，禁止魔法数字散落。
 */
public interface BaseEnum {

    int getCode();

    String getDesc();

    static <E extends Enum<E> & BaseEnum> E of(Class<E> type, Integer code) {
        if (code == null) {
            return null;
        }
        for (E e : type.getEnumConstants()) {
            if (e.getCode() == code) {
                return e;
            }
        }
        throw new IllegalArgumentException(type.getSimpleName() + " 非法枚举值: " + code);
    }
}
