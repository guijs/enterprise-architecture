package com.company.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体：HTTP Status + Result 并存。
 * code 为业务码（0=成功），非 HTTP Status；失败时 data 可承载错误上下文。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    public static final int SUCCESS_CODE = 0;

    /** 业务码：0=成功，非 0=业务失败（不是 HTTP Status）。 */
    private int code;

    private String message;

    /** 成功=业务数据；失败=可选错误上下文（Map / VO / List）。 */
    private T data;

    private String traceId;

    private long timestamp;

    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS_CODE, "success", data, TraceContext.traceId(), System.currentTimeMillis());
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return fail(code, message, null);
    }

    public static <T> Result<T> fail(int code, String message, T data) {
        return new Result<>(code, message, data, TraceContext.traceId(), System.currentTimeMillis());
    }

    public boolean isSuccess() {
        return code == SUCCESS_CODE;
    }
}
