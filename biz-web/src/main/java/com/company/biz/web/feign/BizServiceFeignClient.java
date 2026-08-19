package com.company.biz.web.feign;

import com.company.common.response.Result;
import com.company.log.annotation.FeignLog;
import com.company.log.annotation.LogSwitch;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 调用 biz-service 的内部接口。业务失败由 ErrorDecoder 还原为 BizException；
 * 熔断/超时/连接失败走 Fallback（SERVICE_UNAVAILABLE）。
 */
@FeignClient(name = "biz-service", fallbackFactory = BizServiceFallbackFactory.class)
@FeignLog(response = LogSwitch.ON)
public interface BizServiceFeignClient {

    @GetMapping("/internal/order/{id}")
    Result<OrderDTO> getOrder(@PathVariable Long id);
}
