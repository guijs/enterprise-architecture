package com.company.biz.web.exception;

import com.company.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务域错误码（订单，按模块分段 1xxxx）。message 支持 {key} 命名占位。
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
