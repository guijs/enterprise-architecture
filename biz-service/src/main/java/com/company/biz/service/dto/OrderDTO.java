package com.company.biz.service.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单 DTO：用于内部接口返回，与 Feign 客户端 OrderDTO 同构。
 * 不暴露 Entity 内部细节（如 createdAt、updatedAt、version、deleted 等审计字段）。
 */
@Data
public class OrderDTO {

    private Long id;
    private String orderNo;
    private Long skuId;
    private Integer quantity;
    private BigDecimal amount;
    /** OrderStatus enum name (e.g. "PENDING", "PAID", "CLOSED", "REFUNDED") */
    private String status;
    private String buyerName;
}
