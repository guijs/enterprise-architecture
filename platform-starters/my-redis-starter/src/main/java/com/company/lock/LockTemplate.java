package com.company.lock;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 编程式分布式锁模板，适合动态 key 场景。
 */
@RequiredArgsConstructor
public class LockTemplate {

    private final RedissonClient redissonClient;

    public <T> T execute(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)) {
                throw new LockException("获取锁失败：" + lockKey);
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockException("获取锁被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void execute(String lockKey, Runnable runnable) {
        execute(lockKey, 3, -1, () -> {
            runnable.run();
            return null;
        });
    }
}
