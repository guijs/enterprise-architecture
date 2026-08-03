package com.ea.framework.security;

import com.ea.common.constant.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的登录态缓存
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ea.cache", name = "type", havingValue = "redis", matchIfMissing = true)
public class RedisLoginUserCache implements LoginUserCache {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void put(String token, LoginUser loginUser, long expireSeconds) {
        redisTemplate.opsForValue().set(key(token), loginUser, expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public LoginUser get(String token) {
        Object cached = redisTemplate.opsForValue().get(key(token));
        return cached instanceof LoginUser loginUser ? loginUser : null;
    }

    @Override
    public void remove(String token) {
        redisTemplate.delete(key(token));
    }

    private String key(String token) {
        return Constants.LOGIN_TOKEN_KEY + token;
    }
}
