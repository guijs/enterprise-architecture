-- 为订单表添加 buyer_id 字段用于归属校验
ALTER TABLE `biz_order`
    ADD COLUMN `buyer_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '买家用户ID（归属校验）' AFTER `buyer_name`;

CREATE INDEX `idx_buyer_id` ON `biz_order` (`buyer_id`);
