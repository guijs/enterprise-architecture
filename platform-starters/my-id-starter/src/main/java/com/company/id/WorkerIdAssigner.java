package com.company.id;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * WorkerId 自动分配（Redis）：
 * ① 心跳集合记录占用；② 定时续约保活；③ 心跳过期后回收；④ 重启复用原 workerId。
 */
@Slf4j
@RequiredArgsConstructor
public class WorkerIdAssigner {

    private static final String HEARTBEAT_PREFIX = "id:worker:hb:";
    private static final String INSTANCE_PREFIX = "id:worker:instance:";
    private static final String USED_SET_KEY = "id:worker:used";
    private static final long MAX_WORKER_ID = 1023L;
    private static final long HEARTBEAT_TTL_SEC = 60L;

    private final StringRedisTemplate redisTemplate;
    private final Environment environment;

    @Getter
    private volatile long workerId = -1L;
    private String instanceKey;

    @PostConstruct
    public void init() {
        this.instanceKey = getInstanceKey();
        this.workerId = assignWorkerId();
        log.info("分配 workerId={} for instance={}", workerId, instanceKey);
    }

    private long assignWorkerId() {
        String reuse = redisTemplate.opsForValue().get(INSTANCE_PREFIX + instanceKey);
        if (reuse != null && Boolean.TRUE.equals(redisTemplate.hasKey(HEARTBEAT_PREFIX + reuse))) {
            heartbeat(Long.parseLong(reuse));
            return Long.parseLong(reuse);
        }
        for (long id = 0; id <= MAX_WORKER_ID; id++) {
            if (Boolean.FALSE.equals(redisTemplate.hasKey(HEARTBEAT_PREFIX + id))) {
                Boolean ok = redisTemplate.opsForValue()
                        .setIfAbsent(HEARTBEAT_PREFIX + id, instanceKey, HEARTBEAT_TTL_SEC, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(ok)) {
                    redisTemplate.opsForSet().add(USED_SET_KEY, String.valueOf(id));
                    redisTemplate.opsForValue().set(INSTANCE_PREFIX + instanceKey, String.valueOf(id));
                    return id;
                }
            }
        }
        throw new IdGenerateException("workerId 已耗尽（0~1023 全部占用）");
    }

    /** 定时续约：实例存活期间持续刷新心跳，防止 workerId 被回收。 */
    @Scheduled(fixedRate = 20_000)
    public void renew() {
        if (workerId >= 0) {
            heartbeat(workerId);
        }
    }

    private void heartbeat(long id) {
        redisTemplate.opsForValue().set(HEARTBEAT_PREFIX + id, instanceKey, HEARTBEAT_TTL_SEC, TimeUnit.SECONDS);
    }

    /** 优雅停机时主动释放，加速槽位回收。 */
    @PreDestroy
    public void release() {
        if (workerId >= 0) {
            redisTemplate.delete(HEARTBEAT_PREFIX + workerId);
            redisTemplate.opsForSet().remove(USED_SET_KEY, String.valueOf(workerId));
        }
    }

    private String getInstanceKey() {
        String port = environment.getProperty("server.port", "8080");
        try {
            return InetAddress.getLocalHost().getHostAddress() + ":" + port;
        } catch (UnknownHostException e) {
            return UUID.randomUUID().toString();
        }
    }
}
