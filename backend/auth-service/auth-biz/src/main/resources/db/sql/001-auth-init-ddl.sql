CREATE DATABASE IF NOT EXISTS `auth_db`;
USE `auth_db`;

CREATE TABLE IF NOT EXISTS `auth_users`(
    `id` BIGINT NOT NULL COMMENT '主键 id，雪花算法生成，全局唯一用户标识',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '登录账号（自定义用户名，非必需）',
    `password` VARCHAR(255) NOT NULL COMMENT '密码哈希（BCrypt 加密，第三方登录可为空）',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号（脱敏存储，国际格式+86-13800138000）',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `create_person_id` BIGINT NOT NULL COMMENT '创建人 id',
    `update_person_id` BIGINT NOT NULL COMMENT '更新者 id',
    `status` INT NOT NULL DEFAULT 1 COMMENT '状态：0-冻结，1-正常',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`)
) COMMENT '统一用户身份表';

CREATE TABLE IF NOT EXISTS `auth_user_social`(
    `id` BIGINT NOT NULL COMMENT '主键 id',
    `user_id` BIGINT NOT NULL COMMENT '关联 auth_users.id，一个用户可绑定多个第三方账号',
    `platform` VARCHAR(20) NOT NULL COMMENT 'wechat_mini/wechat_mp/alipay/apple',
    `openid` VARCHAR(100) NOT NULL COMMENT '第三方平台 openid（微信下不同应用 openid 不同）',
    `unionid` VARCHAR(100) DEFAULT NULL COMMENT '微信 unionid（同一主体下多应用统一标识，用于关联多端账号）',
    `platform_nickname` VARCHAR(100) DEFAULT NULL COMMENT '第三方平台昵称（如微信昵称）',
    `platform_avatar` VARCHAR(500) DEFAULT NULL COMMENT '第三方平台头像 URL',
    `access_token` VARCHAR(500) COMMENT '访问令牌（短期）',
    `refresh_token` VARCHAR(500) DEFAULT NULL COMMENT '第三方刷新令牌（长期）',
    `token_expire_time` DATETIME DEFAULT NULL COMMENT 'token 过期时间',
    `extra_data` JSON DEFAULT NULL COMMENT '扩展数据（如微信 session_key，敏感信息加密存储）',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `create_person_id` BIGINT NOT NULL COMMENT '创建人 id',
    `update_person_id` BIGINT NOT NULL COMMENT '更新者 id',
    `deleted` TINYINT NOT NULL COMMENT '逻辑删除:0-未删除，1-已删除 (解绑)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_userid` (`user_id`),
    KEY `idx_unionid` (`unionid`)
) COMMENT '第三方登录绑定表';

CREATE TABLE IF NOT EXISTS `auth_roles`(
    `id` BIGINT NOT NULL COMMENT '主键 id',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码：ROLE_USER, ROLE_ADMIN, ROLE_SUPER',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称：普通用户，超级管理员，店长',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父角色 id（0 表示顶级角色，用于角色继承）',
    `sort` INT DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_parent_id` (`parent_id`)
) COMMENT '角色表';

CREATE TABLE IF NOT EXISTS `auth_permissions`(
    `id` BIGINT NOT NULL COMMENT '主键 id',
    `perm_code` VARCHAR(100) NOT NULL COMMENT '权限编码：system:user:create（用于@PreAuthorize 注解）',
    `perm_name` VARCHAR(100) NOT NULL COMMENT '权限名称：用户新增',
    `type` INT NOT NULL COMMENT '权限类型：1-菜单，2-按钮，3-接口（仅用于分类展示，实际鉴权走注解）',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父权限 id（用于权限树展示）',
    `sort` INT DEFAULT 0 COMMENT '排序号',
    `icon` VARCHAR(50) DEFAULT NULL COMMENT '菜单图标（仅菜单类型使用）',
    `path` VARCHAR(200) COMMENT '路由路径',
    `component` VARCHAR(200) COMMENT '组件路径',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_perm_code` (`perm_code`),
    KEY `idx_parent_type` (`parent_id`, `type`)
) COMMENT '权限定义表';

CREATE TABLE IF NOT EXISTS `auth_user_roles`(
    `id` BIGINT NOT NULL COMMENT '主键 id',
    `user_id` BIGINT NOT NULL COMMENT '用户 id',
    `role_id` BIGINT NOT NULL COMMENT '角色 id',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `create_person_id` BIGINT NOT NULL COMMENT '创建人 id',
    `update_person_id` BIGINT NOT NULL COMMENT '更新者 id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role_client` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) COMMENT '用户角色关联表';

CREATE TABLE IF NOT EXISTS `auth_role_permissions`(
    `id` BIGINT NOT NULL COMMENT '主键 id',
    `role_id` BIGINT NOT NULL COMMENT '角色 id',
    `permission_id` BIGINT NOT NULL COMMENT '权限 id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`),
    KEY `idx_perm_id` (`permission_id`)
) COMMENT '角色权限关联表';
