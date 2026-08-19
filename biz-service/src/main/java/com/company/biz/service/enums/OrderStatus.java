package com.company.biz.service.enums;

import com.company.common.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态：DB 存 code，展示用 desc。
 */
@Getter
@AllArgsConstructor
public enum OrderStatus implements BaseEnum {

    PENDING(10, "待支付"),
    PAID(20, "已支付"),
    CLOSED(30, "已关闭"),
    REFUNDED(40, "已退款");

    private final int code;
    private final String desc;
}
