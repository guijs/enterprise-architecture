package com.company.id;

/**
 * 分布式 ID 生成异常。
 */
public class IdGenerateException extends RuntimeException {

    public IdGenerateException(String message) {
        super(message);
    }
}
