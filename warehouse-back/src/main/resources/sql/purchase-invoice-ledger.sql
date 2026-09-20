-- 采购部：供应商发票查询菜单（可重复执行）
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 262, IFNULL((SELECT pid FROM sys_permission p WHERE p.href = '/business/purchase' LIMIT 1), 194),
       'menu', '供应商发票查询', NULL, 'Ticket', '/business/purchase-invoice', '', 0, 3, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 262 OR href = '/business/purchase-invoice');

INSERT IGNORE INTO sys_role_permission (rid, pid)
SELECT r.id, 262 FROM sys_role r WHERE r.id IN (1, 12);
