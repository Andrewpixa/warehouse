-- 药衡组织树 + 部门主数据（可重复执行）
SET NAMES utf8mb4;

SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dept' AND COLUMN_NAME = 'dept_code') = 0,
    'ALTER TABLE sys_dept ADD COLUMN dept_code VARCHAR(32) DEFAULT NULL COMMENT ''部门编码'' AFTER name',
    'SELECT 1'));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dept' AND COLUMN_NAME = 'dept_type') = 0,
    'ALTER TABLE sys_dept ADD COLUMN dept_type VARCHAR(16) DEFAULT NULL AFTER dept_code',
    'SELECT 1'));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dept' AND COLUMN_NAME = 'manager_user_id') = 0,
    'ALTER TABLE sys_dept ADD COLUMN manager_user_id INT DEFAULT NULL AFTER dept_type',
    'SELECT 1'));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dept' AND COLUMN_NAME = 'phone') = 0,
    'ALTER TABLE sys_dept ADD COLUMN phone VARCHAR(32) DEFAULT NULL AFTER manager_user_id',
    'SELECT 1'));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dept' AND COLUMN_NAME = 'gsp_roles') = 0,
    'ALTER TABLE sys_dept ADD COLUMN gsp_roles VARCHAR(255) DEFAULT NULL AFTER phone',
    'SELECT 1'));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE sys_dept SET name = '药衡医药', dept_code = 'ROOT', dept_type = '公司', remark = NULL, address = NULL, available = 1, open = 1, ordernum = 1, pid = 0 WHERE id = 1;
UPDATE sys_dept SET name = '销售部', dept_code = 'XSB', dept_type = '销售', remark = NULL, address = NULL, available = 1, pid = 1, ordernum = 3 WHERE id = 2;
UPDATE sys_dept SET name = '仓储部', dept_code = 'CCB', dept_type = '仓储', gsp_roles = '收货验收,养护检查,出库复核', remark = '仓储作业与复核', address = NULL, available = 1, pid = 1, ordernum = 4 WHERE id = 3;
UPDATE sys_dept SET name = '质量部', dept_code = 'ZLB', dept_type = '质量', gsp_roles = '首营审核,放行 / 停售解控,不合格品管理', remark = '质量放行与不合格品', address = NULL, available = 1, pid = 1, ordernum = 5 WHERE id = 4;

INSERT INTO sys_dept (id, pid, name, dept_code, dept_type, open, remark, address, available, ordernum, createtime, gsp_roles)
SELECT 21, 1, '总经办', 'ZJB', '总经办', 0, NULL, NULL, 1, 2, NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 21 OR dept_code = 'ZJB');
INSERT INTO sys_dept (id, pid, name, dept_code, dept_type, open, remark, address, available, ordernum, createtime, gsp_roles)
SELECT 22, 1, '采购部', 'CGB', '采购', 0, NULL, NULL, 1, 6, NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 22 OR dept_code = 'CGB');
INSERT INTO sys_dept (id, pid, name, dept_code, dept_type, open, remark, address, available, ordernum, createtime, gsp_roles)
SELECT 23, 1, '财务部', 'CWB', '财务', 0, NULL, NULL, 1, 7, NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 23 OR dept_code = 'CWB');
INSERT INTO sys_dept (id, pid, name, dept_code, dept_type, open, remark, address, available, ordernum, createtime, gsp_roles)
SELECT 24, 1, '信息部', 'XXB', '信息', 0, NULL, NULL, 1, 8, NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 24 OR dept_code = 'XXB');
INSERT INTO sys_dept (id, pid, name, dept_code, dept_type, open, remark, address, available, ordernum, createtime, gsp_roles)
SELECT 27, 24, 'IT组', 'XXB-IT', '信息', 0, '模拟开票与系统运维', NULL, 1, 1, NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 27 OR dept_code = 'XXB-IT');
INSERT INTO sys_dept (id, pid, name, dept_code, dept_type, open, remark, address, available, ordernum, createtime, gsp_roles)
SELECT 28, 24, '物流组', 'XXB-WL', '物流', 0, '物流联系与发运协同', NULL, 1, 2, NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 28 OR dept_code = 'XXB-WL');
INSERT INTO sys_dept (id, pid, name, dept_code, dept_type, open, remark, address, available, ordernum, createtime, gsp_roles)
SELECT 25, 3, '常温仓组', 'CCB-CW', '仓储', 0, '常温作业组', NULL, 1, 1, NOW(), '收货验收,出库复核'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 25 OR dept_code = 'CCB-CW');
INSERT INTO sys_dept (id, pid, name, dept_code, dept_type, open, remark, address, available, ordernum, createtime, gsp_roles)
SELECT 26, 3, '冷链仓组', 'CCB-LL', '仓储', 0, '冷链作业组', NULL, 1, 2, NOW(), '收货验收,养护检查,出库复核'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 26 OR dept_code = 'CCB-LL');

UPDATE sys_dept SET pid = 2, name = '销售一部', dept_code = 'XSB-1', dept_type = '销售', available = 1, remark = NULL, address = NULL, ordernum = 1 WHERE id = 5;
UPDATE sys_dept SET pid = 2, name = '销售二部', dept_code = 'XSB-2', dept_type = '销售', available = 1, remark = NULL, address = NULL, ordernum = 2 WHERE id = 6;

UPDATE sys_dept SET available = 0, remark = '历史节点已停用'
WHERE id IN (7,8,9,10,18) AND (dept_code IS NULL OR dept_code NOT IN ('ROOT','ZJB','XSB','CCB','ZLB','CGB','CWB','XXB','CCB-CW','CCB-LL','XSB-1','XSB-2'));
