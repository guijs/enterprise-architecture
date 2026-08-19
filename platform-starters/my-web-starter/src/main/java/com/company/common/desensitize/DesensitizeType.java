package com.company.common.desensitize;

/**
 * 脱敏类型（展示层脱敏，区别于存储层加密 my-crypto-starter）。
 */
public enum DesensitizeType {
    MOBILE,
    ID_CARD,
    EMAIL,
    BANK_CARD,
    NAME,
    PASSWORD,
    CUSTOM
}
