-- 订单表：遵循 6.30 数据库设计规范（InnoDB + utf8mb4，Snowflake 主键，审计/乐观锁/逻辑删除字段）
CREATE TABLE IF NOT EXISTS `biz_order`
(
    `id`          BIGINT UNSIGNED NOT NULL COMMENT '主键（Snowflake）',
    `order_no`    VARCHAR(64)     NOT NULL COMMENT '订单号',
    `sku_id`      BIGINT UNSIGNED NOT NULL COMMENT '商品 SKU',
    `quantity`    INT             NOT NULL DEFAULT 1 COMMENT '数量',
    `amount`      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00 COMMENT '金额',
    `status`      INT             NOT NULL DEFAULT 10 COMMENT '状态：10待支付 20已支付 30已关闭 40已退款',
    `buyer_name`  VARCHAR(64)              DEFAULT NULL COMMENT '下单人',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(64)              DEFAULT NULL COMMENT '创建人',
    `update_by`   VARCHAR(64)              DEFAULT NULL COMMENT '更新人',
    `version`     INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_sku_status` (`sku_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='订单表';
