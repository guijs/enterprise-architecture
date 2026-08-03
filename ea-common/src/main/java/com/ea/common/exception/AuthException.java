package com.ea.common.exception;

import com.ea.common.enums.ResultCode;
import lombok.Getter;

/**
 * 认证/授权异常
 */
@Getter
public class AuthException extends RuntimeException {

    private final int code;

    public AuthException(String message) {
        super(message);
        this.code = ResultCode.UNAUTHORIZED.getCode();
    }

    public AuthException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public AuthException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
