-- 追溯码包装层级解析：大包装 → 中包装 → 最小包装。可重复执行。
SET NAMES utf8mb4;

SET @col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trace_codes' AND COLUMN_NAME = 'parent_code'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE trace_codes ADD COLUMN parent_code VARCHAR(64) DEFAULT NULL COMMENT ''上级包装码'' AFTER pack_level',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS trace_pack_relations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  parent_code VARCHAR(64) NOT NULL COMMENT '上级码',
  child_code VARCHAR(64) NOT NULL COMMENT '下级码',
  parent_level VARCHAR(20) NOT NULL COMMENT '大包装/中包装',
  child_level VARCHAR(20) NOT NULL COMMENT '中包装/最小包装',
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_trace_pack_child (child_code),
  KEY idx_trace_pack_parent (parent_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='追溯码包装关联（课设预置，模拟码上放心解析）';

-- 演示：1 箱 = 2 中包 = 6 小盒
INSERT INTO trace_pack_relations (parent_code, child_code, parent_level, child_level, remark)
SELECT v.parent_code, v.child_code, v.parent_level, v.child_level, v.remark
FROM (
  SELECT '81000000000000000001' AS parent_code, '82000000000000000001' AS child_code, '大包装' AS parent_level, '中包装' AS child_level, '演示箱→中包A' AS remark
  UNION ALL SELECT '81000000000000000001', '82000000000000000002', '大包装', '中包装', '演示箱→中包B'
  UNION ALL SELECT '82000000000000000001', '83000000000000000001', '中包装', '最小包装', '演示中包A→小盒'
  UNION ALL SELECT '82000000000000000001', '83000000000000000002', '中包装', '最小包装', '演示中包A→小盒'
  UNION ALL SELECT '82000000000000000001', '83000000000000000003', '中包装', '最小包装', '演示中包A→小盒'
  UNION ALL SELECT '82000000000000000002', '83000000000000000004', '中包装', '最小包装', '演示中包B→小盒'
  UNION ALL SELECT '82000000000000000002', '83000000000000000005', '中包装', '最小包装', '演示中包B→小盒'
  UNION ALL SELECT '82000000000000000002', '83000000000000000006', '中包装', '最小包装', '演示中包B→小盒'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM trace_pack_relations r WHERE r.child_code = v.child_code
);

-- 菜单挂在销售出库下；操作权限独立，不再复用 sales:*
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 238, 4, 'menu', '追溯码查询', NULL, 'Connection', '/business/trace', '', 0, 4, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 238 OR href = '/business/trace');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 239, 238, 'permission', '追溯码查看', 'trace:view', NULL, NULL, NULL, 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 239 OR percode = 'trace:view');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 240, 238, 'permission', '追溯码采集', 'trace:collect', NULL, NULL, NULL, 0, 2, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 240 OR percode = 'trace:collect');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 241, 238, 'permission', '包装解析', 'trace:parse', NULL, NULL, NULL, 0, 3, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 241 OR percode = 'trace:parse');

-- 仓管要看到「销售出库」分组，才能点进追溯码菜单
INSERT INTO sys_role_permission (rid, pid)
SELECT 11, 4 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE rid = 11 AND pid = 4);

-- 查看：超管 / 仓管 / 销售
INSERT INTO sys_role_permission (rid, pid)
SELECT r.id, p.id
FROM sys_role r
JOIN (SELECT 238 AS id UNION SELECT 239) p
WHERE r.id IN (1, 11, 13)
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.rid = r.id AND rp.pid = p.id);

-- 采集 + 解析：仅超管、仓管
INSERT INTO sys_role_permission (rid, pid)
SELECT r.id, p.id
FROM sys_role r
JOIN (SELECT 240 AS id UNION SELECT 241) p
WHERE r.id IN (1, 11)
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.rid = r.id AND rp.pid = p.id);
