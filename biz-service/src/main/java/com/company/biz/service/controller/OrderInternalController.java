package com.company.biz.service.controller;

import com.company.biz.service.entity.OrderEntity;
import com.company.biz.service.service.OrderService;
import com.company.common.page.PageQuery;
import com.company.common.page.PageResult;
import com.company.common.response.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

/**
 * 内部接口：仅供网关后的其它服务经 Feign 调用，需内网隔离，不可被外部直接访问。
 * 返回 DTO 而非 Entity，符合 PR #6 契约。
 */
@RestController
@RequestMapping("/internal/order")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public Result<OrderDTO> getOrder(@PathVariable Long id) {
        return Result.ok(toDTO(orderService.getById(id)));
    }

    @PostMapping
    public Result<Long> createOrder(@RequestBody @Valid OrderCreateCmd cmd) {
        OrderEntity order = new OrderEntity();
        order.setOrderNo(cmd.getOrderNo());
        order.setSkuId(cmd.getSkuId());
        order.setQuantity(cmd.getQuantity());
        order.setAmount(cmd.getAmount());
        return Result.ok(orderService.create(order));
    }

    @GetMapping
    public Result<PageResult<OrderDTO>> page(@Valid PageQuery query) {
        PageResult<OrderEntity> entityPage = orderService.page(query);
        PageResult<OrderDTO> dtoPage = new PageResult<>(
                entityPage.getRecords().stream().map(this::toDTO).collect(Collectors.toList()),
                entityPage.getTotal(),
                entityPage.getPageNum(),
                entityPage.getPageSize()
        );
        return Result.ok(dtoPage);
    }

    private OrderDTO toDTO(OrderEntity entity) {
        OrderDTO dto = new OrderDTO();
        dto.setId(entity.getId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setSkuId(entity.getSkuId());
        dto.setQuantity(entity.getQuantity());
        dto.setAmount(entity.getAmount());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().getCode() : null);
        dto.setBuyerId(entity.getBuyerId());
        dto.setBuyerName(entity.getBuyerName());
        return dto;
    }
}
