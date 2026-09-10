package com.company.biz.web.feign;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单请求 DTO（跨服务传输对象）。
 */
@Data
public class OrderCreateDTO {

    private String orderNo;
    private Long skuId;
    private Integer quantity;
    private BigDecimal amount;
    private String buyerName;
}
