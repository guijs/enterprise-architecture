package com.company.redis;

import org.springframework.beans.factory.annotation.Value;

/**
 * key 命名规范工具：{appName}:{业务模块}:{具体key}，避免多服务共用 Redis 冲突。
 */
public class RedisKeyHelper {

    @Value("${spring.application.name:app}")
    private String appName;

    public String buildKey(String module, String key) {
        return appName + ":" + module + ":" + key;
    }
}
