-- 补「批号库存」侧栏菜单（K-01）。可重复执行。
-- 挂在仓储质量分组（id=194）下，授权超管与仓管。
SET NAMES utf8mb4;

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 242, 194, 'menu', '批号库存', NULL, 'Box', '/business/batch-stock', '', 0, 2, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 242 OR href = '/business/batch-stock');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 243, 242, 'permission', '批号库存查看', 'batchStock:view', NULL, NULL, NULL, 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 243 OR percode = 'batchStock:view');

INSERT IGNORE INTO sys_role_permission (rid, pid)
SELECT r.id, p.id
FROM sys_role r
JOIN (SELECT 242 AS id UNION SELECT 243) p
WHERE r.id IN (1, 11);
