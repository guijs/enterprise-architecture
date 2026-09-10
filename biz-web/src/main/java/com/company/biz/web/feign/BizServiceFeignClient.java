package com.company.biz.web.feign;

import com.company.common.page.PageResult;
import com.company.common.response.Result;
import com.company.log.annotation.FeignLog;
import com.company.log.annotation.LogSwitch;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 调用 biz-service 的内部接口。业务失败由 ErrorDecoder 还原为 BizException；
 * 熔断/超时/连接失败走 Fallback（SERVICE_UNAVAILABLE）。
 */
@FeignClient(name = "biz-service", fallbackFactory = BizServiceFallbackFactory.class)
@FeignLog(response = LogSwitch.ON)
public interface BizServiceFeignClient {

    @GetMapping("/api/internal/order/{id}")
    Result<OrderDTO> getOrder(@PathVariable Long id);

    @PostMapping("/api/internal/order")
    Result<Long> createOrder(@RequestBody OrderCreateCmd cmd);

    @GetMapping("/api/internal/order")
    Result<PageResult<OrderDTO>> pageOrders(@SpringQueryMap OrderPageQuery query);
}
