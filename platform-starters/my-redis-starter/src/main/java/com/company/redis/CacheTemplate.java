package com.company.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 通用缓存模板：Cache-Aside + 防穿透（空值）+ 防击穿（回源加锁）+ 防雪崩（TTL 抖动）。
 */
@Slf4j
@RequiredArgsConstructor
public class CacheTemplate {

    private static final String NULL_VALUE = "\u0000NULL\u0000";

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public <T> T getWithCache(String key, Class<T> type, Duration ttl, Duration nullTtl, Supplier<T> dbLoader) {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return NULL_VALUE.equals(cached) ? null : deserialize(cached, type);
        }

        RLock lock = redissonClient.getLock("cache:lock:" + key);
        try {
            lock.lock(3, TimeUnit.SECONDS);
            cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return NULL_VALUE.equals(cached) ? null : deserialize(cached, type);
            }
            T value = dbLoader.get();
            if (value == null) {
                redisTemplate.opsForValue().set(key, NULL_VALUE, nullTtl);
                return null;
            }
            long jitter = ThreadLocalRandom.current().nextLong(0, ttl.toSeconds() / 5 + 1);
            redisTemplate.opsForValue().set(key, serialize(value), ttl.plusSeconds(jitter));
            return value;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("缓存反序列化失败", e);
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("缓存序列化失败", e);
        }
    }
}
