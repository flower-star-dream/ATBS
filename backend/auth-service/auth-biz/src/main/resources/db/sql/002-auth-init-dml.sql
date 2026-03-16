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
(8, 'system:role:list', '角色查询', 3, 7, 1, NULL, NULL);

-- 角色权限关联（超管拥有所有权限）
INSERT INTO auth_role_permissions (id, role_id, permission_id) VALUES
(1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 1, 4), (5, 1, 5), (6, 1, 6), (7, 1, 7), (8, 1, 8);

-- 初始化超管用户（密码：admin123）
INSERT INTO auth_users (id, username, password, phone, status, create_time, update_time, create_person_id, update_person_id) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '13800138000', 1, NOW(), NOW(), 0, 0);

INSERT INTO auth_user_roles (id, user_id, role_id, create_time, update_time, create_person_id, update_person_id) VALUES
(1, 1, 1, NOW(), NOW(), 0, 0);