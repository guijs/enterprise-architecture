package com.company.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务异常：与 Result 对齐，持有业务码 + HTTP Status + 可选错误上下文 data。
 * data 支持 Map / VO / List；Map 场景可用于占位符填充 message。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    /** 给前端的错误上下文：Map / VO / List，可为 null。 */
    private final transient Object data;

    public BizException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), errorCode.getHttpStatus(), null);
    }

    public BizException(ErrorCode errorCode, Object data) {
        this(errorCode.getCode(), resolveMessage(errorCode.getMessage(), data), errorCode.getHttpStatus(), data);
    }

    public BizException(ErrorCode errorCode, HttpStatus httpStatus, Object data) {
        this(errorCode.getCode(), resolveMessage(errorCode.getMessage(), data), httpStatus, data);
    }

    public BizException(int code, String message, HttpStatus httpStatus, Object data) {
        super(message);
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus == null ? HttpStatus.BAD_REQUEST : httpStatus;
        this.data = data;
    }

    /** 便捷构造：默认 HTTP 400，无附加 data。 */
    public BizException(int code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST, null);
    }

    /** 便捷构造：指定 HTTP，无附加 data。 */
    public BizException(int code, String message, HttpStatus httpStatus) {
        this(code, message, httpStatus, null);
    }

    /** 命名占位 + 参数进 data（推荐）。 */
    public static BizException of(ErrorCode errorCode, Map<String, ?> params) {
        String msg = ErrorMessageFormatter.format(errorCode.getMessage(), params);
        return new BizException(errorCode.getCode(), msg, errorCode.getHttpStatus(), params);
    }

    /** 顺序占位 {}；args 仅用于填文案，默认不进 data。 */
    public static BizException of(ErrorCode errorCode, Object... args) {
        String msg = ErrorMessageFormatter.format(errorCode.getMessage(), args);
        return new BizException(errorCode.getCode(), msg, errorCode.getHttpStatus(), null);
    }

    /** 顺序占位，同时把 args 按 arg0/arg1... 放进 data（少用，优先命名 Map）。 */
    public static BizException ofWithArgs(ErrorCode errorCode, Object... args) {
        String msg = ErrorMessageFormatter.format(errorCode.getMessage(), args);
        Map<String, Object> data = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            data.put("arg" + i, args[i]);
        }
        return new BizException(errorCode.getCode(), msg, errorCode.getHttpStatus(), data);
    }

    @SuppressWarnings("unchecked")
    private static String resolveMessage(String template, Object data) {
        if (data instanceof Map<?, ?> map) {
            return ErrorMessageFormatter.format(template, (Map<String, ?>) map);
        }
        return template;
    }
}
