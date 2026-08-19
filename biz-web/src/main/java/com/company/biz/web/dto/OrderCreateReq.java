package com.company.biz.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建订单请求 DTO（带校验注解）。
 */
@Data
@Schema(description = "创建订单请求")
public class OrderCreateReq {

    @NotNull
    @Schema(description = "商品 SKU")
    private Long skuId;

    @NotNull
    @Min(1)
    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "订单号（幂等/审计用）")
    private String orderNo;
}
