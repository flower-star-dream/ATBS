CREATE DATABASE IF NOT EXISTS `order_db`;
USE `order_db`;
-- order_db.atbs_order definition

CREATE TABLE IF NOT EXISTS `atbs_order` (
  `id` bigint NOT NULL COMMENT '订单编号',
  `user_id` bigint NOT NULL COMMENT '订购用户 id',
  `status` int NOT NULL DEFAULT '0' COMMENT '订单支付状态',
  `remarks` varchar(100) DEFAULT NULL COMMENT '订单备注',
  `total_price` decimal(10,2) NOT NULL COMMENT '总价',
  `amount_paid` decimal(10,2) DEFAULT NULL COMMENT '已付金额',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `create_time` datetime NOT NULL COMMENT '绑定时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_person_id` bigint NOT NULL COMMENT '创建人 id（通常为 0，表示用户自主绑定）',
  `update_person_id` bigint NOT NULL COMMENT '更新者 id',
  `deleted` tinyint NOT NULL COMMENT '逻辑删除:0-未删除，1-已删除（解绑）',
  `version` int NOT NULL COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='atbs_订单';