-- 采购入库：把开单/确认拆开，供管理端按角色授权。可重复执行。
SET NAMES utf8mb4;

-- 采购入库单菜单可见，权限挂在其下，角色管理里才能勾选
UPDATE sys_permission SET available = 1, title = '采购入库单' WHERE id = 10;
UPDATE sys_permission SET pid = 10, available = 1 WHERE id IN (168, 169, 170, 171, 172);
UPDATE sys_permission SET title = '查看采购入库单' WHERE id = 168;
UPDATE sys_permission SET title = '开采购入库单' WHERE id = 169;
UPDATE sys_permission SET title = '修改采购入库单' WHERE id = 170;
UPDATE sys_permission SET title = '删除采购入库单' WHERE id = 171;

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 234, 10, 'permission', '确认入库', 'inport:confirm', NULL, NULL, NULL, 0, 6, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 234 OR percode = 'inport:confirm');

-- 仓库管理员可确认入库；采购员默认只有开单，不授确认
INSERT IGNORE INTO sys_role_permission (rid, pid)
SELECT r.id, 234
FROM sys_role r
WHERE r.id IN (1, 11);
