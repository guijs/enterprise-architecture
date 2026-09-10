package com.company.biz.service.controller;

import com.company.biz.service.dto.OrderCreateCmd;
import com.company.biz.service.dto.OrderDTO;
import com.company.biz.service.entity.OrderEntity;
import com.company.biz.service.service.OrderService;
import com.company.common.response.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部接口：仅供网关后的其它服务经 Feign 调用，需内网隔离，不可被外部直接访问。
 * 
 * 契约约定：
 * - 返回 DTO 而非 Entity，避免数据库实体泄露至调用方
 * - 业务错误通过 BizException 抛出，由 GlobalExceptionHandler 统一处理为 HTTP 4xx + Result body
 * - 调用方 Feign ErrorDecoder 解析 Result body 并还原为 BizException
 */
@RestController
@RequestMapping("/internal/order")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public Result<OrderDTO> getOrder(@PathVariable Long id) {
        OrderEntity entity = orderService.getById(id);
        return Result.ok(toDTO(entity));
    }

    @PostMapping
    public Result<Long> createOrder(@RequestBody @Valid OrderCreateCmd cmd) {
        OrderEntity entity = toEntity(cmd);
        Long orderId = orderService.create(entity);
        return Result.ok(orderId);
    }

    private OrderDTO toDTO(OrderEntity entity) {
        if (entity == null) {
            return null;
        }
        OrderDTO dto = new OrderDTO();
        dto.setId(entity.getId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setSkuId(entity.getSkuId());
        dto.setQuantity(entity.getQuantity());
        dto.setAmount(entity.getAmount());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setBuyerName(entity.getBuyerName());
        return dto;
    }

    private OrderEntity toEntity(OrderCreateCmd cmd) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderNo(cmd.getOrderNo());
        entity.setSkuId(cmd.getSkuId());
        entity.setQuantity(cmd.getQuantity());
        entity.setAmount(cmd.getAmount());
        entity.setBuyerName(cmd.getBuyerName());
        return entity;
    }
}
