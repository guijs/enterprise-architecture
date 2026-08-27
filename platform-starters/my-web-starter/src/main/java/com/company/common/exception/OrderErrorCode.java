package com.company.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 订单业务错误码（1xxxx）。message 支持 {key} 命名占位。
 * 共享错误码，biz-web 和 biz-service 均可引用。
 */
@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    STOCK_NOT_ENOUGH(10001, "商品{skuId}库存不足，当前可用{available}，需要{required}", HttpStatus.CONFLICT),
    ORDER_NOT_FOUND(10004, "订单不存在", HttpStatus.NOT_FOUND),
    ORDER_STATUS_INVALID(10009, "订单状态不允许此操作，当前状态={status}", HttpStatus.UNPROCESSABLE_ENTITY);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
