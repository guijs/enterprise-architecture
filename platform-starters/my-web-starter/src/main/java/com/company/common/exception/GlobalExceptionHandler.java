package com.company.common.exception;

import cn.hutool.core.util.StrUtil;
import com.company.common.response.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器：用 ResponseEntity 写出对应 HTTP Status，body 统一为 Result。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Object>> handleBizException(BizException e) {
        log.warn("业务异常：http={}, code={}, message={}", e.getHttpStatus().value(), e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus())
                .body(Result.fail(e.getCode(), e.getMessage(), e.getData()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Object>> handleValidException(MethodArgumentNotValidException e) {
        List<Map<String, String>> fields = e.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", StrUtil.nullToDefault(err.getDefaultMessage(), "不合法")))
                .toList();
        return badRequest(fields);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Object>> handleBindException(BindException e) {
        List<Map<String, String>> fields = e.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", StrUtil.nullToDefault(err.getDefaultMessage(), "不合法")))
                .toList();
        return badRequest(fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Object>> handleConstraintViolation(ConstraintViolationException e) {
        List<Map<String, String>> fields = e.getConstraintViolations().stream()
                .map(v -> Map.of(
                        "field", v.getPropertyPath().toString(),
                        "message", v.getMessage()))
                .toList();
        return badRequest(fields);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(CommonErrorCode.SYSTEM_ERROR.getCode(), CommonErrorCode.SYSTEM_ERROR.getMessage()));
    }

    private ResponseEntity<Result<Object>> badRequest(List<Map<String, String>> fields) {
        String message = fields.stream().map(m -> m.get("message")).collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(CommonErrorCode.BAD_REQUEST.getCode(), message, fields));
    }
}
