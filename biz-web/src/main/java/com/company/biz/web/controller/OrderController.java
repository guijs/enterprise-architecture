package com.company.biz.web.controller;

import com.company.biz.web.dto.OrderCreateReq;
import com.company.biz.web.feign.BizServiceFeignClient;
import com.company.biz.web.feign.OrderDTO;
import com.company.biz.web.vo.OrderVO;
import com.company.common.exception.BizException;
import com.company.common.page.PageQuery;
import com.company.common.page.PageResult;
import com.company.common.response.Result;
import com.company.idempotent.Idempotent;
import com.company.log.annotation.LogSwitch;
import com.company.log.annotation.OperationLog;
import com.company.log.annotation.RequestLog;
import com.company.ratelimit.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单接口示例：串联限流、幂等、审计、接口日志与 Feign 调用。
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@RequestLog(response = LogSwitch.OFF)
@Tag(name = "订单接口")
public class OrderController {

    private final BizServiceFeignClient bizServiceFeignClient;

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public Result<OrderDTO> detail(@PathVariable Long id) {
        Result<OrderDTO> result = bizServiceFeignClient.getOrder(id);
        if (result == null || !result.isSuccess()) {
            throw new BizException(com.company.common.exception.CommonErrorCode.SYSTEM_ERROR);
        }
        return result;
    }

    @PostMapping
    @Operation(summary = "创建订单")
    @RateLimit(limit = 10, window = 1, timeUnit = TimeUnit.MINUTES, message = "操作过于频繁")
    @Idempotent(key = "#req.orderNo", cacheResult = true)
    @OperationLog(module = "订单", type = "CREATE", content = "创建订单：#{#req.orderNo}")
    @RequestLog(response = LogSwitch.ON)
    public Result<Long> create(@RequestBody @Valid OrderCreateReq req) {
        // 骨架：实际下单逻辑委托给 Service / 下游服务
        return Result.ok(System.currentTimeMillis());
    }

    @GetMapping
    @Operation(summary = "订单分页")
    public Result<PageResult<OrderVO>> page(@Valid PageQuery query) {
        return Result.ok(new PageResult<>(List.of(), 0, query.getPageNum(), query.getPageSize()));
    }
}
