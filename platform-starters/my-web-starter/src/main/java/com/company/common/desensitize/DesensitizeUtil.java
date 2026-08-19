package com.company.common.desensitize;

import cn.hutool.core.util.StrUtil;

/**
 * 脱敏工具：接口出参、日志统一复用，禁止明文落日志。
 */
public final class DesensitizeUtil {

    private DesensitizeUtil() {
    }

    public static String desensitize(String value, DesensitizeType type, int prefixKeep, int suffixKeep) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        return switch (type) {
            case MOBILE -> mask(value, 3, 4);
            case ID_CARD -> mask(value, 3, 4);
            case EMAIL -> maskEmail(value);
            case BANK_CARD -> mask(value, 0, 4);
            case NAME -> maskName(value);
            case PASSWORD -> "******";
            case CUSTOM -> mask(value, prefixKeep, suffixKeep);
        };
    }

    private static String mask(String value, int prefix, int suffix) {
        int len = value.length();
        if (len <= prefix + suffix) {
            return StrUtil.repeat('*', len);
        }
        String head = value.substring(0, prefix);
        String tail = value.substring(len - suffix);
        return head + StrUtil.repeat('*', len - prefix - suffix) + tail;
    }

    private static String maskName(String name) {
        if (name.length() <= 1) {
            return name;
        }
        return name.charAt(0) + StrUtil.repeat('*', name.length() - 1);
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
