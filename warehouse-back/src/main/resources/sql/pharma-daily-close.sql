-- 日结检查菜单（只读清单，不锁账）
SET NAMES utf8mb4;

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 225, 194, 'menu', '日结检查', NULL, 'Calendar', '/business/daily-close', '', 0, 7, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 225 OR href = '/business/daily-close');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 226, 225, 'permission', '日结查看', 'dailyClose:view', NULL, NULL, NULL, 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 226 OR percode = 'dailyClose:view');

INSERT IGNORE INTO sys_role_permission (rid, pid)
SELECT r.id, p.id
FROM sys_role r
JOIN (
  SELECT 225 AS id UNION SELECT 226
) p
WHERE r.id IN (1, 11);
