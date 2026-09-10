package com.company.biz.web.feign;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单命令对象（调用内部接口使用）。
 */
@Data
public class OrderCreateCmd {

    private Long skuId;

    private Integer quantity;

    private String orderNo;

    private BigDecimal amount;
}
