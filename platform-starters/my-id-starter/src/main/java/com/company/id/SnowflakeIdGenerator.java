package com.company.id;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Snowflake 核心算法。
 * 用 ReentrantLock 而非 synchronized：语义清晰、可控，且规避虚拟线程 pinning。
 */
public class SnowflakeIdGenerator {

    private static final long EPOCH = 1704067200000L; // 2024-01-01 00:00:00 UTC
    private static final long WORKER_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_BITS);   // 1023
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);  // 4095
    private static final long WORKER_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS;

    private final long workerId;
    private final ReentrantLock lock = new ReentrantLock();

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId 超出范围: " + workerId);
        }
        this.workerId = workerId;
    }

    public long nextId() {
        lock.lock();
        try {
            return doNextId();
        } finally {
            lock.unlock();
        }
    }

    private long doNextId() {
        long current = System.currentTimeMillis();
        if (current < lastTimestamp) {
            long offset = lastTimestamp - current;
            if (offset <= 5) {
                try {
                    Thread.sleep(offset << 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                current = System.currentTimeMillis();
            } else {
                throw new IdGenerateException("时钟回拨过大，差值=" + offset + "ms");
            }
        }

        if (current == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                current = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = current;
        return ((current - EPOCH) << TIMESTAMP_SHIFT) | (workerId << WORKER_SHIFT) | sequence;
    }

    private long waitNextMillis(long last) {
        long ts = System.currentTimeMillis();
        while (ts <= last) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }
}
