package com.company.biz.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.biz.service.enums.OrderStatus;
import com.company.mybatis.BaseEntity;
import com.company.mybatis.BaseEnumTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单实体：继承 BaseEntity（Snowflake 主键、审计字段、乐观锁、逻辑删除）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "biz_order", autoResultMap = true)
public class OrderEntity extends BaseEntity {

    private String orderNo;

    private Long skuId;

    private Integer quantity;

    private BigDecimal amount;

    @TableField(typeHandler = BaseEnumTypeHandler.class)
    private OrderStatus status;

    private String buyerName;
}
