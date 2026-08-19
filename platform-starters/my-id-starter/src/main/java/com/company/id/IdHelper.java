package com.company.id;

import lombok.RequiredArgsConstructor;

/**
 * 对外 ID 工具类。前端用 String 避免 JS 精度丢失。
 */
@RequiredArgsConstructor
public class IdHelper {

    private final SnowflakeIdGenerator generator;

    public long nextId() {
        return generator.nextId();
    }

    public String nextIdStr() {
        return String.valueOf(generator.nextId());
    }
}
