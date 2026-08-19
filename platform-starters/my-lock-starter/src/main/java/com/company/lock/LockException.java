package com.company.lock;

/**
 * 获取分布式锁失败异常（默认异常类型，需有 String 构造器）。
 */
public class LockException extends RuntimeException {

    public LockException(String message) {
        super(message);
    }
}
