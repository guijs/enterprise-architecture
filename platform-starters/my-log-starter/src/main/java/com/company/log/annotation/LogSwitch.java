package com.company.log.annotation;

/**
 * 三态开关：解决 boolean 无法表达「跟随上一级」。
 */
public enum LogSwitch {
    /** 跟随上一级：方法 → 类 → 全局。 */
    DEFAULT,
    ON,
    OFF
}
