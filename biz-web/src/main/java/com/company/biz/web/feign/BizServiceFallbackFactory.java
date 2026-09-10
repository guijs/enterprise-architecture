package com.company.biz.web.feign;

import com.company.common.exception.BizException;
import com.company.common.exception.CommonErrorCode;
import com.company.common.page.PageResult;
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
        if (cause instanceof BizException bizEx) {
            return new BizServiceFeignClient() {
                @Override
                public Result<OrderDTO> getOrder(Long id) {
                    throw bizEx;
                }

                @Override
                public Result<Long> createOrder(OrderCreateCmd cmd) {
                    throw bizEx;
                }

                @Override
                public Result<PageResult<OrderDTO>> pageOrders(OrderPageQuery query) {
                    throw bizEx;
                }
            };
        }

        return new BizServiceFeignClient() {
            @Override
            public Result<OrderDTO> getOrder(Long id) {
                log.error("Feign 基础设施降级，getOrder orderId={}", id, cause);
                throw new BizException(CommonErrorCode.SERVICE_UNAVAILABLE);
            }

            @Override
            public Result<Long> createOrder(OrderCreateCmd cmd) {
                log.error("Feign 基础设施降级，createOrder orderNo={}", cmd.getOrderNo(), cause);
                throw new BizException(CommonErrorCode.SERVICE_UNAVAILABLE);
            }

            @Override
            public Result<PageResult<OrderDTO>> pageOrders(OrderPageQuery query) {
                log.error("Feign 基础设施降级，pageOrders", cause);
                throw new BizException(CommonErrorCode.SERVICE_UNAVAILABLE);
            }
        };
    }
}
