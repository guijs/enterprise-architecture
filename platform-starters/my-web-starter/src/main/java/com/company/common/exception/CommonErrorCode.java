package com.company.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 平台公共错误码。业务码分段建议：
 * 0 成功 · 4xxxx 平台通用客户端错误 · 5xxxx 平台系统/依赖错误 · 各业务域自洽（订单 1xxxx、用户 2xxxx …）。
 */
@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    SUCCESS(0, "success", HttpStatus.OK),
    BAD_REQUEST(40000, "请求参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "未登录或登录已失效", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "无权限", HttpStatus.FORBIDDEN),
    NOT_FOUND(40400, "资源不存在", HttpStatus.NOT_FOUND),
    TOO_MANY_REQUESTS(42900, "请求过于频繁", HttpStatus.TOO_MANY_REQUESTS),
    SYSTEM_ERROR(50000, "系统繁忙，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(50300, "下游服务暂不可用", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
