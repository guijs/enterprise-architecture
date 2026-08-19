package com.company.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 错误码契约：code 为业务码（进 Result），另挂 httpStatus 决定 HTTP 状态。
 */
public interface ErrorCode {

    /** Result.code（业务码），不是 HTTP Status。 */
    int getCode();

    String getMessage();

    default HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
