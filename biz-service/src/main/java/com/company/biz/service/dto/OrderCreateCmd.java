package com.company.biz.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单命令 DTO：用于内部接口接收，与 Feign 客户端 OrderCreateDTO 同构。
 * 命名为 Cmd（Command）表示这是一个写操作命令对象。
 */
@Data
public class OrderCreateCmd {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "SKU ID 不能为空")
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;

    @NotNull(message = "金额不能为空")
    @Min(value = 0, message = "金额不能为负")
    private BigDecimal amount;

    private String buyerName;
}
