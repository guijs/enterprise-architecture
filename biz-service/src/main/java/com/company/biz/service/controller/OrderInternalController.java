package com.company.biz.service.controller;

import com.company.biz.service.entity.OrderEntity;
import com.company.biz.service.service.OrderService;
import com.company.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部接口：仅供网关后的其它服务经 Feign 调用，需内网隔离，不可被外部直接访问。
 */
@RestController
@RequestMapping("/internal/order")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public Result<OrderEntity> getOrder(@PathVariable Long id) {
        return Result.ok(orderService.getById(id));
    }

    @PostMapping
    public Result<Long> createOrder(@RequestBody OrderEntity order) {
        Long orderId = orderService.create(order);
        return Result.ok(orderId);
    }
}
