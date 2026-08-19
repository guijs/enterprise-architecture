package com.company.security;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Optional;

/**
 * 基于 TransmittableThreadLocal 的用户上下文：
 * 原生 ThreadLocal 在 @Async / 线程池场景会丢失，使用阿里 TTL 解决跨线程传递。
 */
public final class UserContext {

    private static final TransmittableThreadLocal<UserInfo> CONTEXT = new TransmittableThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserInfo user) {
        CONTEXT.set(user);
    }

    public static UserInfo get() {
        return CONTEXT.get();
    }

    public static String getUserId() {
        return Optional.ofNullable(CONTEXT.get()).map(UserInfo::getUserId).orElse(null);
    }

    public static String getUserName() {
        return Optional.ofNullable(CONTEXT.get()).map(UserInfo::getUserName).orElse(null);
    }

    /** 请求结束必须调用，防止线程池复用时信息污染。 */
    public static void remove() {
        CONTEXT.remove();
    }
}
