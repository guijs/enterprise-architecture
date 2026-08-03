package com.ea.framework.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地内存登录态缓存（仅建议 local 环境）
 */
@Component
@ConditionalOnProperty(prefix = "ea.cache", name = "type", havingValue = "memory")
public class MemoryLoginUserCache implements LoginUserCache {

    private final Map<String, CacheItem> store = new ConcurrentHashMap<>();

    @Override
    public void put(String token, LoginUser loginUser, long expireSeconds) {
        long expireAt = System.currentTimeMillis() + expireSeconds * 1000;
        store.put(token, new CacheItem(loginUser, expireAt));
    }

    @Override
    public LoginUser get(String token) {
        CacheItem item = store.get(token);
        if (item == null) {
            return null;
        }
        if (item.expireAt() < System.currentTimeMillis()) {
            store.remove(token);
            return null;
        }
        return item.loginUser();
    }

    @Override
    public void remove(String token) {
        store.remove(token);
    }

    private record CacheItem(LoginUser loginUser, long expireAt) {
    }
}
