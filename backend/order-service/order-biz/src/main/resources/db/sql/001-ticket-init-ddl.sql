CREATE DATABASE IF NOT EXISTS `order_db`;
USE `order_db`;
-- order_db.atbs_ticket definition

CREATE TABLE IF NOT EXISTS `atbs_ticket` (
  `id` bigint NOT NULL COMMENT '票号',
  `order_id` bigint NOT NULL COMMENT '所属订单号',
  `passenger_id` bigint NOT NULL COMMENT '乘车人 id',
  `seat_reservation_id` bigint NOT NULL COMMENT '座位预订号',
  `status` int NOT NULL DEFAULT '0' COMMENT '票使用状态',
  `money` decimal(10,2) NOT NULL COMMENT '票价',
  `start_time` datetime NOT NULL COMMENT '出发时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `start_station_id` varchar(50) NOT NULL COMMENT '出发站 id',
  `end_station_id` varchar(50) NOT NULL COMMENT '到达站 id',
  `create_time` datetime NOT NULL COMMENT '绑定时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_person_id` bigint NOT NULL COMMENT '创建人 id（通常为 0，表示用户自主绑定）',
  `update_person_id` bigint NOT NULL COMMENT '更新者 id',
  `deleted` tinyint NOT NULL COMMENT '逻辑删除:0-未删除，1-已删除',
  `vision` int NOT NULL COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='atbs_车票';
