-- 发货确认 + 医院收货：出库单补发货时间/电子发票/签收状态
SET NAMES utf8mb4;

SET @col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders' AND COLUMN_NAME = 'receive_status'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE sales_orders
     ADD COLUMN ship_time DATETIME NULL COMMENT ''发货时间'' AFTER confirmed_at,
     ADD COLUMN einvoice_no VARCHAR(32) DEFAULT NULL COMMENT ''电子发票号'' AFTER ship_time,
     ADD COLUMN einvoice_path VARCHAR(255) DEFAULT NULL COMMENT ''电子发票文件'' AFTER einvoice_no,
     ADD COLUMN receive_status VARCHAR(16) DEFAULT NULL COMMENT ''待收货/已签收/部分签收/拒收'' AFTER einvoice_path,
     ADD COLUMN received_at DATETIME NULL COMMENT ''医院签收时间'' AFTER receive_status,
     ADD COLUMN received_by BIGINT DEFAULT NULL COMMENT ''签收人'' AFTER received_at,
     ADD COLUMN receive_remark VARCHAR(255) DEFAULT NULL COMMENT ''收货备注'' AFTER received_by',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_order_items' AND COLUMN_NAME = 'received_qty'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE sales_order_items
     ADD COLUMN received_qty DECIMAL(18,3) DEFAULT NULL COMMENT ''医院实收数量'' AFTER qty,
     ADD COLUMN receive_remark VARCHAR(255) DEFAULT NULL COMMENT ''行收货备注'' AFTER received_qty',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sales_orders
SET ship_time = IFNULL(ship_time, confirmed_at),
    einvoice_no = IFNULL(NULLIF(einvoice_no, ''), invoice_no),
    receive_status = CASE
      WHEN IFNULL(order_type, '正常') = '红冲' THEN NULL
      WHEN status <> '已确认' THEN NULL
      WHEN IFNULL(paid_status, '未回款') = '已回款' THEN '已签收'
      ELSE '待收货'
    END
WHERE status = '已确认'
  AND (receive_status IS NULL OR receive_status = '');

UPDATE sales_order_items i
JOIN sales_orders o ON o.id = i.order_id
SET i.received_qty = i.qty
WHERE o.receive_status = '已签收'
  AND i.received_qty IS NULL;

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 235, 4, 'menu', '医院收货确认', NULL, 'Checked', '/business/receipt', '', 0, 3, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 235 OR href = '/business/receipt');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 236, 235, 'permission', '收货查看', 'receipt:view', NULL, NULL, NULL, 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 236 OR percode = 'receipt:view');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 237, 235, 'permission', '确认收货', 'receipt:confirm', NULL, NULL, NULL, 0, 2, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 237 OR percode = 'receipt:confirm');

INSERT IGNORE INTO sys_role_permission (rid, pid)
SELECT r.id, p.id
FROM sys_role r
JOIN (
  SELECT 235 AS id UNION SELECT 236 UNION SELECT 237
) p
WHERE r.id IN (1, 11, 13);
