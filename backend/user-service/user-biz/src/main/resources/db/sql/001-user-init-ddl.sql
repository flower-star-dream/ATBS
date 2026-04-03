CREATE DATABASE IF NOT EXISTS `user_db`;
USE `user_db`;
-- user_db.atbs_employee definition

CREATE TABLE IF NOT EXISTS `atbs_employee` (
  `id` bigint NOT NULL COMMENT '统一身份认证 id',
  `username` varchar(30) NOT NULL COMMENT '用户名',
  `nickname` varchar(30) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(200) DEFAULT NULL COMMENT '头像 url',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态',
  `phone` varchar(11) NOT NULL COMMENT '手机号',
  `permission_level` varchar(10) NOT NULL COMMENT '权限等级',
  `affiliated_site` varchar(100) DEFAULT NULL COMMENT '所属站点',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_person_id` bigint NOT NULL COMMENT '创建人 id',
  `update_person_id` bigint NOT NULL COMMENT '更新者 id',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除:0-未删除，1-已删除',
  `version` int NOT NULL DEFAULT '1' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='atbs_员工';


-- user_db.atbs_passenger definition

CREATE TABLE IF NOT EXISTS `atbs_passenger` (
  `id` bigint NOT NULL COMMENT '乘车人 id',
  `real_name` varchar(100) NOT NULL COMMENT '真实姓名',
  `card_type` varchar(10) NOT NULL COMMENT '证件类型',
  `id_card` varchar(19) NOT NULL COMMENT '身份证',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_person_id` bigint NOT NULL COMMENT '创建人 id',
  `update_person_id` bigint NOT NULL COMMENT '更新者 id',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除:0-未删除，1-已删除',
  `version` int NOT NULL DEFAULT '1' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='atbs_乘客';


-- user_db.atbs_user definition

CREATE TABLE IF NOT EXISTS `atbs_user` (
  `id` bigint NOT NULL COMMENT '统一身份认证 id',
  `openid` varchar(45) NOT NULL COMMENT '微信用户唯一标识',
  `username` varchar(30) NOT NULL COMMENT '用户名',
  `avatar` varchar(200) DEFAULT NULL COMMENT '头像 url',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(11) DEFAULT NULL COMMENT '手机号',
  `passenger_id` bigint DEFAULT NULL COMMENT '乘客 id',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_person_id` bigint NOT NULL COMMENT '创建人 id',
  `update_person_id` bigint NOT NULL COMMENT '更新者 id',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除:0-未删除，1-已删除',
  `version` int NOT NULL DEFAULT '1' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='atbs_用户';


-- user_db.atbs_user_passenger definition

CREATE TABLE IF NOT EXISTS `atbs_user_passenger` (
  `id` bigint NOT NULL COMMENT '用户乘客映射 id',
  `user_id` bigint NOT NULL COMMENT '用户 id',
  `passenger_id` bigint NOT NULL COMMENT '乘客 id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='atbs_用户与乘客';