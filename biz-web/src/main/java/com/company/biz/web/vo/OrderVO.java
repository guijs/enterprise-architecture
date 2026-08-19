package com.company.biz.web.vo;

import com.company.common.desensitize.Desensitize;
import com.company.common.desensitize.DesensitizeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应 VO：敏感字段脱敏输出。
 */
@Data
@Schema(description = "订单详情")
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long skuId;

    private Integer quantity;

    private BigDecimal amount;

    @Schema(description = "下单人（脱敏）")
    @Desensitize(type = DesensitizeType.NAME)
    private String buyerName;

    @Schema(description = "联系电话（脱敏）")
    @Desensitize(type = DesensitizeType.MOBILE)
    private String mobile;

    private LocalDateTime createTime;
}
