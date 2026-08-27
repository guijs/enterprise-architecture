package com.company.biz.web.feign;

import com.company.common.exception.BizException;
import com.company.common.exception.CommonErrorCode;
import com.company.common.response.Result;
import org.springframework.cloud.openfeign.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback 仅处理基础设施降级；下游业务错误（已是 BizException）应直接抛出，不被覆盖。
 */
@Slf4j
@Component
public class BizServiceFallbackFactory implements FallbackFactory<BizServiceFeignClient> {

    @Override
    public BizServiceFeignClient create(Throwable cause) {
        return new BizServiceFeignClient() {
            @Override
            public Result<OrderDTO> getOrder(Long id) {
                return handleFallback(cause, "getOrder", id);
            }

            @Override
            public Result<Long> createOrder(OrderCreateDTO order) {
                return handleFallback(cause, "createOrder", order.getOrderNo());
            }

            private <T> Result<T> handleFallback(Throwable cause, String method, Object arg) {
                if (cause instanceof BizException bizEx) {
                    throw bizEx;
                }
                log.error("Feign 基础设施降级，method={}, arg={}", method, arg, cause);
                throw new BizException(CommonErrorCode.SERVICE_UNAVAILABLE);
            }
        };
    }
}
