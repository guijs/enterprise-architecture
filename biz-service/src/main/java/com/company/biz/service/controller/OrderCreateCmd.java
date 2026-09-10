package com.company.biz.service.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单命令对象（内部接口使用）。
 * 注意：buyerId/buyerName 由服务端从 UserContext 获取，不从请求中接收。
 */
@Data
public class OrderCreateCmd {

    @NotNull
    private Long skuId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String orderNo;

    private BigDecimal amount;
}
