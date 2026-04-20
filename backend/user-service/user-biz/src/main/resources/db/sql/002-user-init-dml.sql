USE `user_db`;

-- 初始化员工数据
INSERT INTO atbs_employee (id, username, nickname, avatar, status, phone, permission_level, affiliated_site, create_time, update_time, create_person_id, update_person_id, deleted, version) VALUES
(1, 'system', '超级管理员', 'http://103.115.43.55:9000/assets/atbs/Avatar.png', 1, '13728016558', 'ROLE_SUPER', 'all', '2025-01-01 00:00:00', '2025-01-01 00:00:00', 0, 0, 0, 1),
(2, 'admin', '管理员', 'http://103.115.43.55:9000/assets/atbs/Avatar.png', 1, '13800138001', 'ROLE_ADMIN', 'all', '2025-01-01 00:00:00', '2025-01-01 00:00:00', 1, 1, 0, 1),
(3, 'employee001', '北京站员工', 'http://103.115.43.55:9000/assets/atbs/Avatar.png', 1, '13800138010', 'ROLE_USER', '北京首都国际机场', '2025-01-01 00:00:00', '2025-01-01 00:00:00', 1, 1, 0, 1),
(4, 'employee002', '上海站员工', 'http://103.115.43.55:9000/assets/atbs/Avatar.png', 1, '13800138011', 'ROLE_USER', '上海浦东国际机场', '2025-01-01 00:00:00', '2025-01-01 00:00:00', 1, 1, 0, 1),
(5, 'employee003', '广州站员工', 'http://103.115.43.55:9000/assets/atbs/Avatar.png', 1, '13800138012', 'ROLE_USER', '广州白云国际机场', '2025-01-01 00:00:00', '2025-01-01 00:00:00', 1, 1, 0, 1);

-- 初始化乘客数据
INSERT INTO atbs_passenger (id, real_name, card_type, id_card, create_time, update_time, create_person_id, update_person_id, deleted, version) VALUES
(1, '张三', '身份证', '110101199001011234', '2025-01-01 00:00:00', '2025-01-01 00:00:00', 3, 3, 0, 1),
(2, '李四', '身份证', '310101199002022345', '2025-01-01 00:00:00', '2025-01-01 00:00:00', 4, 4, 0, 1),
(3, '王五', '身份证', '440101199003033456', '2025-01-01 00:00:00', '2025-01-01 00:00:00', 5, 5, 0, 1),
(4, '赵六', '身份证', '510101199004044567', '2025-01-01 00:00:00', '2025-01-01 00:00:00', 6, 6, 0, 1),
(5, '孙七', '身份证', '330101199005055678', '2025-01-01 00:00:00', '2025-01-01 00:00:00', 7, 7, 0, 1),
(6, '周八', '护照', 'G12345678', '2025-01-01 00:00:00', '2025-01-01 00:00:00', 3, 3, 0, 1),
(7, '吴九', '身份证', '610101199006066789', '2025-01-01 00:00:00', '2025-01-01 00:00:00', 4, 4, 0, 1),
(8, '郑十', '身份证', '500101199007077890', '2025-01-01 00:00:00', '2025-01-01 00:00:00', 5, 5, 0, 1);

-- 初始化用户数据
INSERT INTO atbs_user (id, openid, username, avatar, email, phone, passenger_id, status, create_time, update_time, create_person_id, update_person_id, deleted, version) VALUES
(3, 'oTest001', 'zhangsan', 'https://avatar.example.com/zhangsan.jpg', 'zhangsan@atbs.com', '13800138002', 1, 1, '2025-01-01 00:00:00', '2025-01-01 00:00:00', 3, 3, 0, 1),
(4, 'oTest002', 'lisi', 'https://avatar.example.com/lisi.jpg', 'lisi@atbs.com', '13800138003', 2, 1, '2025-01-01 00:00:00', '2025-01-01 00:00:00', 4, 4, 0, 1),
(5, 'oTest003', 'wangwu', 'https://avatar.example.com/wangwu.jpg', 'wangwu@atbs.com', '13800138004', 3, 1, '2025-01-01 00:00:00', '2025-01-01 00:00:00', 5, 5, 0, 1),
(6, 'oTest004', 'zhaoliu', 'https://avatar.example.com/zhaoliu.jpg', 'zhaoliu@atbs.com', '13800138005', 4, 1, '2025-01-01 00:00:00', '2025-01-01 00:00:00', 6, 6, 0, 1),
(7, 'oTest005', 'sunqi', 'https://avatar.example.com/sunqi.jpg', 'sunqi@atbs.com', '13800138006', 5, 1, '2025-01-01 00:00:00', '2025-01-01 00:00:00', 7, 7, 0, 1);

-- 初始化用户乘客关联数据（用户可以绑定多个乘客）
INSERT INTO atbs_user_passenger (id, user_id, passenger_id) VALUES
(1, 3, 1),
(2, 3, 6),
(3, 4, 2),
(4, 5, 3),
(5, 5, 7),
(6, 6, 4),
(7, 6, 8),
(8, 7, 5);
