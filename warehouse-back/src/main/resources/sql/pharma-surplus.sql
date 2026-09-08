-- 批号升益：按药品 + 仓库 + 批号记升益量/升益额（及损耗）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS surplus_orders (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL,
  warehouse_id BIGINT NOT NULL,
  biz_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT '草稿',
  total_surplus_qty DECIMAL(18,3) NOT NULL DEFAULT 0,
  total_surplus_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  total_loss_qty DECIMAL(18,3) NOT NULL DEFAULT 0,
  total_loss_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  created_by BIGINT DEFAULT NULL,
  confirmed_by BIGINT DEFAULT NULL,
  confirmed_at DATETIME DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_surplus_order_no (order_no),
  KEY idx_surplus_warehouse (warehouse_id),
  KEY idx_surplus_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批号升益单';

CREATE TABLE IF NOT EXISTS surplus_order_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  drug_id BIGINT NOT NULL,
  batch_no VARCHAR(64) NOT NULL,
  quality_status VARCHAR(20) NOT NULL DEFAULT '合格',
  book_qty DECIMAL(18,3) NOT NULL DEFAULT 0,
  actual_qty DECIMAL(18,3) NOT NULL DEFAULT 0,
  surplus_qty DECIMAL(18,3) NOT NULL DEFAULT 0,
  loss_qty DECIMAL(18,3) NOT NULL DEFAULT 0,
  unit_cost DECIMAL(18,4) NOT NULL DEFAULT 0,
  surplus_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  loss_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  production_date DATE DEFAULT NULL,
  expire_date DATE DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_surplus_item_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批号升益单明细';

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 220, 194, 'menu', '批号升益', NULL, 'Plus', '/business/surplus', '', 0, 6, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 220 OR href = '/business/surplus');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 221, 220, 'permission', '升益查看', 'surplus:view', NULL, NULL, NULL, 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 221 OR percode = 'surplus:view');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 222, 220, 'permission', '升益开单', 'surplus:create', NULL, NULL, NULL, 0, 2, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 222 OR percode = 'surplus:create');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 223, 220, 'permission', '升益确认', 'surplus:confirm', NULL, NULL, NULL, 0, 3, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 223 OR percode = 'surplus:confirm');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 224, 220, 'permission', '升益删除', 'surplus:delete', NULL, NULL, NULL, 0, 4, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 224 OR percode = 'surplus:delete');

INSERT IGNORE INTO sys_role_permission (rid, pid)
SELECT r.id, p.id
FROM sys_role r
JOIN (
  SELECT 220 AS id UNION SELECT 221 UNION SELECT 222 UNION SELECT 223 UNION SELECT 224
) p
WHERE r.id IN (1, 11);
