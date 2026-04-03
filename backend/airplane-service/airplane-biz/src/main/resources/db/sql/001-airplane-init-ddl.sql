CREATE DATABASE IF NOT EXISTS `airplane_db`;
USE `airplane_db`;
-- airplane_db.atbs_airplane definition

CREATE TABLE IF NOT EXISTS `atbs_airplane` (
  `id` bigint NOT NULL COMMENT '飞机号',
  `airplane_name` varchar(10) NOT NULL COMMENT '飞机名',
  `airplane_model` varchar(50) NOT NULL COMMENT '飞机型号',
  `seat_num` int NOT NULL COMMENT '座位数',
  `service_years` int NOT NULL COMMENT '服务年数',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_person_id` bigint NOT NULL COMMENT '创建人 id',
  `update_person_id` bigint NOT NULL COMMENT '更新者 id',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除:0-未删除，1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='atbs_飞机';


-- airplane_db.atbs_route definition

CREATE TABLE IF NOT EXISTS `atbs_route` (
  `id` bigint NOT NULL COMMENT '线路号',
  `route_name` varchar(20) NOT NULL COMMENT '线路名',
  `start_station_id` bigint NOT NULL COMMENT '起点站 id',
  `end_station_id` bigint NOT NULL COMMENT '终点站 id',
  `station_count` int NOT NULL COMMENT '站点数',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_person_id` bigint NOT NULL COMMENT '创建人 id',
  `update_person_id` bigint NOT NULL COMMENT '更新者 id',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除:0-未删除，1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='atbs_航线';


-- airplane_db.atbs_route_stations definition

CREATE TABLE IF NOT EXISTS `atbs_route_stations` (
  `id` bigint NOT NULL COMMENT '路线站点号',
  `route_id` bigint NOT NULL COMMENT '线路号',
  `station_id` bigint NOT NULL COMMENT '站点号',
  `station_sorting` int NOT NULL COMMENT '站点顺序',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='atbs_航线站点表';


-- airplane_db.atbs_schedule definition

CREATE TABLE IF NOT EXISTS `atbs_schedule` (
  `id` bigint NOT NULL COMMENT '班次号',
  `train_id` bigint NOT NULL COMMENT '飞机号',
  `route_id` bigint NOT NULL COMMENT '航线号',
  `available_tickets` int NOT NULL COMMENT '余票',
  `start_time` datetime NOT NULL COMMENT '出发时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `captain` varchar(10) DEFAULT NULL COMMENT '机长',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_person_id` bigint NOT NULL COMMENT '创建人 id',
  `update_person_id` bigint NOT NULL COMMENT '更新者 id',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除:0-未删除，1-已删除',
  `version` int NOT NULL DEFAULT '1' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='atbs_航班班次';


-- airplane_db.atbs_seat_reservation definition

CREATE TABLE IF NOT EXISTS `atbs_seat_reservation` (
  `id` bigint NOT NULL COMMENT '座位预订号',
  `schedule_id` bigint NOT NULL COMMENT '班次号',
  `seat_number` int NOT NULL COMMENT '座位号',
  `booking_status` int NOT NULL COMMENT '预订状态',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_person_id` bigint NOT NULL COMMENT '创建人 id',
  `update_person_id` bigint NOT NULL COMMENT '更新者 id',
  `version` int NOT NULL DEFAULT '1' COMMENT '乐观锁版本号',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除:0-未删除，1-已删除',
  `status` int NOT NULL COMMENT '预订状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='atbs_座位预订';


-- airplane_db.atbs_station definition

CREATE TABLE IF NOT EXISTS `atbs_station` (
  `id` bigint NOT NULL COMMENT '站点号',
  `station_name` varchar(50) NOT NULL COMMENT '站点名',
  `address` varchar(100) NOT NULL COMMENT '地址',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_person_id` bigint NOT NULL COMMENT '创建人 id',
  `update_person_id` bigint NOT NULL COMMENT '更新者 id',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除:0-未删除，1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='atbs_站点';