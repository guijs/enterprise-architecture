CREATE TABLE IF NOT EXISTS `biz_order`
(
    `id`          BIGINT NOT NULL,
    `order_no`    VARCHAR(64)     NOT NULL,
    `sku_id`      BIGINT NOT NULL,
    `quantity`    INT             NOT NULL DEFAULT 1,
    `amount`      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    `status`      INT             NOT NULL DEFAULT 10,
    `buyer_name`  VARCHAR(64)              DEFAULT NULL,
    `buyer_id`    BIGINT                   DEFAULT NULL,
    `create_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_by`   VARCHAR(64)              DEFAULT NULL,
    `update_by`   VARCHAR(64)              DEFAULT NULL,
    `version`     INT             NOT NULL DEFAULT 0,
    `deleted`     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);
