USE `auth_db`;

-- 初始化角色
INSERT INTO auth_roles (id, role_code, role_name, parent_id, sort) VALUES
(1, 'ROLE_SUPER', '超级管理员', 0, 0),
(2, 'ROLE_ADMIN', '管理员', 1, 1),
(3, 'ROLE_USER', '普通用户', 0, 2);

-- 初始化权限
INSERT INTO auth_permissions (id, perm_code, perm_name, type, parent_id, sort, path, component) VALUES
(1, 'system', '系统管理', 1, 0, 0, '/system', NULL),
(2, 'system:user', '用户管理', 1, 1, 1, '/system/user', 'system/user/index'),
(3, 'system:user:list', '用户查询', 3, 2, 1, NULL, NULL),
(4, 'system:user:add', '用户新增', 3, 2, 2, NULL, NULL),
(5, 'system:user:edit', '用户编辑', 3, 2, 3, NULL, NULL),
(6, 'system:user:delete', '用户删除', 3, 2, 4, NULL, NULL),
(7, 'system:role', '角色管理', 1, 1, 2, '/system/role', 'system/role/index'),
(8, 'system:role:list', '角色查询', 3, 7, 1, NULL, NULL),
(9, 'airplane', '航班管理', 1, 0, 1, '/airplane', NULL),
(10, 'airplane:schedule', '班次管理', 1, 9, 1, '/airplane/schedule', 'airplane/schedule/index'),
(11, 'airplane:schedule:list', '班次查询', 3, 10, 1, NULL, NULL),
(12, 'airplane:schedule:add', '班次新增', 3, 10, 2, NULL, NULL),
(13, 'airplane:schedule:edit', '班次编辑', 3, 10, 3, NULL, NULL),
(14, 'airplane:schedule:delete', '班次删除', 3, 10, 4, NULL, NULL),
(15, 'order', '订单管理', 1, 0, 2, '/order', NULL),
(16, 'order:list', '订单查询', 3, 15, 1, NULL, NULL);

-- 角色权限关联（超管拥有所有权限）
INSERT INTO auth_role_permissions (id, role_id, permission_id) VALUES
(1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 1, 4), (5, 1, 5), (6, 1, 6), (7, 1, 7), (8, 1, 8),
(9, 1, 9), (10, 1, 10), (11, 1, 11), (12, 1, 12), (13, 1, 13), (14, 1, 14),
(15, 1, 15), (16, 1, 16);

-- 管理员角色权限关联
INSERT INTO auth_role_permissions (id, role_id, permission_id) VALUES
(17, 2, 1), (18, 2, 2), (19, 2, 3), (20, 2, 4), (21, 2, 5),
(22, 2, 9), (23, 2, 10), (24, 2, 11), (25, 2, 12), (26, 2, 13),
(27, 2, 15), (28, 2, 16);

-- 普通用户角色权限关联
INSERT INTO auth_role_permissions (id, role_id, permission_id) VALUES
(29, 3, 15), (30, 3, 16);

-- 初始化超管用户（密码：Atbs20260329）
INSERT INTO auth_users (id, username, password, phone, email, status, create_time, update_time, create_person_id, update_person_id) VALUES
(1, 'system', '$2a$12$mL6/BQ/q9ZrfTQ0K.KmbreM50TWo5etO4COHTjKUTEmh6/vrIST3i', '13728016558', '1378281299@qq.com', 1, NOW(), NOW(), 1, 1);

-- 初始化管理员用户（密码：Admin20260329）
INSERT INTO auth_users (id, username, password, phone, email, status, create_time, update_time, create_person_id, update_person_id) VALUES
(2, 'admin', '$2a$12$mL6/BQ/q9ZrfTQ0K.KmbreM50TWo5etO4COHTjKUTEmh6/vrIST3i', '13800138001', 'admin@atbs.com', 1, NOW(), NOW(), 1, 1);

-- 初始化普通用户（密码：User20260329）
INSERT INTO auth_users (id, username, password, phone, email, status, create_time, update_time, create_person_id, update_person_id) VALUES
(3, 'zhangsan', '$2a$12$mL6/BQ/q9ZrfTQ0K.KmbreM50TWo5etO4COHTjKUTEmh6/vrIST3i', '13800138002', 'zhangsan@atbs.com', 1, NOW(), NOW(), 1, 1),
(4, 'lisi', '$2a$12$mL6/BQ/q9ZrfTQ0K.KmbreM50TWo5etO4COHTjKUTEmh6/vrIST3i', '13800138003', 'lisi@atbs.com', 1, NOW(), NOW(), 1, 1),
(5, 'wangwu', '$2a$12$mL6/BQ/q9ZrfTQ0K.KmbreM50TWo5etO4COHTjKUTEmh6/vrIST3i', '13800138004', 'wangwu@atbs.com', 1, NOW(), NOW(), 1, 1),
(6, 'zhaoliu', '$2a$12$mL6/BQ/q9ZrfTQ0K.KmbreM50TWo5etO4COHTjKUTEmh6/vrIST3i', '13800138005', 'zhaoliu@atbs.com', 1, NOW(), NOW(), 1, 1),
(7, 'sunqi', '$2a$12$mL6/BQ/q9ZrfTQ0K.KmbreM50TWo5etO4COHTjKUTEmh6/vrIST3i', '13800138006', 'sunqi@atbs.com', 1, NOW(), NOW(), 1, 1);

-- 初始化用户角色关联
INSERT INTO auth_user_roles (id, user_id, role_id, create_time, update_time, create_person_id, update_person_id) VALUES
(1, 1, 1, NOW(), NOW(), 0, 0),
(2, 2, 2, NOW(), NOW(), 1, 1),
(3, 3, 3, NOW(), NOW(), 1, 1),
(4, 4, 3, NOW(), NOW(), 1, 1),
(5, 5, 3, NOW(), NOW(), 1, 1),
(6, 6, 3, NOW(), NOW(), 1, 1),
(7, 7, 3, NOW(), NOW(), 1, 1);

-- 初始化第三方登录绑定数据
INSERT INTO auth_user_social (id, user_id, platform, openid, unionid, platform_nickname, platform_avatar, access_token, refresh_token, token_expire_time, extra_data, create_time, update_time, create_person_id, update_person_id, deleted) VALUES
(1, 3, 'wechat_mini', 'oTest001', 'uTest001', '张三', 'https://avatar.example.com/zhangsan.jpg', NULL, NULL, NULL, NULL, NOW(), NOW(), 3, 3, 0),
(2, 4, 'wechat_mini', 'oTest002', 'uTest002', '李四', 'https://avatar.example.com/lisi.jpg', NULL, NULL, NULL, NULL, NOW(), NOW(), 4, 4, 0),
(3, 5, 'wechat_mp', 'oTest003', 'uTest003', '王五', 'https://avatar.example.com/wangwu.jpg', NULL, NULL, NULL, NULL, NOW(), NOW(), 5, 5, 0),
(4, 6, 'wechat_mini', 'oTest004', 'uTest004', '赵六', 'https://avatar.example.com/zhaoliu.jpg', NULL, NULL, NULL, NULL, NOW(), NOW(), 6, 6, 0),
(5, 7, 'wechat_mini', 'oTest005', 'uTest005', '孙七', 'https://avatar.example.com/sunqi.jpg', NULL, NULL, NULL, NULL, NOW(), NOW(), 7, 7, 0);
