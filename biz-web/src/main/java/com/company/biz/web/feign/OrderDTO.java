package com.company.biz.web.feign;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 跨服务传输对象（biz-service 内部接口返回）。
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
}
