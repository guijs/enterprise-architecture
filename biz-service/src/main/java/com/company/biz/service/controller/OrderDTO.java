package com.company.biz.service.controller;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单 DTO（内部接口返回）。
 * 与 biz-web 的 OrderDTO 保持同构，避免直接暴露 Entity。
 */
@Data
public class OrderDTO {

    private Long id;
    private String orderNo;
    private Long skuId;
    private Integer quantity;
    private BigDecimal amount;
    private Integer status;
    private Long buyerId;
    private String buyerName;
}
