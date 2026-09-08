-- 日清 / 月结 / 进货出货统计：表、菜单、演示数据（可重复执行）

CREATE TABLE IF NOT EXISTS daily_close_records (
  id BIGINT NOT NULL AUTO_INCREMENT,
  biz_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT '已日清',
  purchase_count INT NOT NULL DEFAULT 0,
  purchase_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  outbound_count INT NOT NULL DEFAULT 0,
  outbound_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  closed_by BIGINT DEFAULT NULL,
  closed_by_name VARCHAR(64) DEFAULT NULL,
  closed_at DATETIME DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_daily_close_biz_date (biz_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日清记录';

CREATE TABLE IF NOT EXISTS monthly_close_records (
  id BIGINT NOT NULL AUTO_INCREMENT,
  close_month CHAR(7) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT '已月结',
  customer_count INT NOT NULL DEFAULT 0,
  sales_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  unpaid_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  supplier_count INT NOT NULL DEFAULT 0,
  purchase_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  closed_by BIGINT DEFAULT NULL,
  closed_by_name VARCHAR(64) DEFAULT NULL,
  closed_at DATETIME DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_monthly_close_ym (close_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月结记录';

UPDATE sys_permission SET title = '日清检查' WHERE id = 225;

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 233, 225, 'permission', '日清确认', 'dailyClose:confirm', NULL, NULL, NULL, 0, 2, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 233 OR percode = 'dailyClose:confirm');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 227, 194, 'menu', '月结对账', NULL, 'Notebook', '/business/monthly-close', '', 0, 8, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 227 OR href = '/business/monthly-close');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 228, 227, 'permission', '月结查看', 'monthlyClose:view', NULL, NULL, NULL, 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 228 OR percode = 'monthlyClose:view');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 229, 227, 'permission', '月结确认', 'monthlyClose:confirm', NULL, NULL, NULL, 0, 2, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 229 OR percode = 'monthlyClose:confirm');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 230, 194, 'menu', '进货统计', NULL, 'DataAnalysis', '/business/purchase-stats', '', 0, 9, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 230 OR href = '/business/purchase-stats');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 231, 194, 'menu', '出货统计', NULL, 'TrendCharts', '/business/outbound-stats', '', 0, 10, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 231 OR href = '/business/outbound-stats');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 232, 230, 'permission', '进销统计查看', 'pharmaStats:view', NULL, NULL, NULL, 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 232 OR percode = 'pharmaStats:view');

INSERT IGNORE INTO sys_role_permission (rid, pid)
SELECT r.id, p.id
FROM sys_role r
JOIN (
  SELECT 227 AS id UNION SELECT 228 UNION SELECT 229 UNION SELECT 230 UNION SELECT 231 UNION SELECT 232 UNION SELECT 233
) p
WHERE r.id IN (1, 11, 14);

-- 测试单据：拉开日期，便于统计图；09-01 已日清；09-08 可点日清
INSERT INTO purchase_orders (id, order_no, supplier_id, warehouse_id, salesman_id, checker_id, keeper_id, biz_date, check_result, status, total_amount, created_by, confirmed_by, confirmed_at, remark, created_at, updated_at)
VALUES
  (5, 'CG20260903001', 200001, 1, 2, 5, 3, '2026-09-03', '合格', '已确认', 860.00, 1, 1, '2026-09-03 10:00:00', '补货阿莫西林', NOW(), NOW()),
  (6, 'CG20260908001', 200003, 1, 2, 5, 3, '2026-09-08', '合格', '已确认', 410.00, 1, 1, '2026-09-08 09:30:00', '今日入库演示', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), total_amount = VALUES(total_amount), biz_date = VALUES(biz_date);

INSERT INTO purchase_order_items (id, order_id, drug_id, batch_no, production_date, expire_date, receive_qty, qualified_qty, stock_in_qty, purchase_price, amount, quality_status, spdid, remark, created_at, updated_at)
VALUES
  (8, 5, 300001, 'AMX20260801', '2026-08-01', '2028-07-31', 100.000, 100.000, 100.000, 8.60, 860.00, '合格', 'SPD20260903008', NULL, NOW(), NOW()),
  (9, 6, 300005, 'GC20260501', '2026-05-01', '2028-04-30', 100.000, 100.000, 100.000, 4.10, 410.00, '合格', 'SPD20260908009', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE stock_in_qty = VALUES(stock_in_qty), amount = VALUES(amount);

INSERT INTO sales_orders (id, order_no, invoice_no, customer_id, warehouse_id, salesman_id, reviewer_id, biz_date, pay_type, status, total_amount, paid_status, paid_amount, created_by, confirmed_by, confirmed_at, remark, created_at, updated_at)
VALUES
  (6, 'CK20260903001', '20260903000100000006', 100002, 1, 4, 5, '2026-09-03', '现结', '已确认', 256.00, '已回款', 256.00, 1, 1, '2026-09-03 15:00:00', '大参林现结', NOW(), NOW()),
  (7, 'CK20260904001', '20260904000100000007', 100001, 1, 4, 5, '2026-09-04', '月结', '已确认', 384.00, '未回款', 0.00, 1, 1, '2026-09-04 11:00:00', '南方医院月结挂账', NOW(), NOW()),
  (8, 'CK20260908001', '20260908000100000008', 100002, 1, 4, 5, '2026-09-08', '现结', '已确认', 128.00, '已回款', 128.00, 1, 1, '2026-09-08 10:40:00', '今日现结已回，可日清', NOW(), NOW())
ON DUPLICATE KEY UPDATE invoice_no = VALUES(invoice_no), paid_status = VALUES(paid_status), status = VALUES(status);

INSERT INTO sales_order_items (id, order_id, drug_id, batch_no, expire_date, qty, sale_price, amount, spdid, quality_status, remark, created_at, updated_at)
VALUES
  (7, 6, 300001, 'AMX20260801', '2028-07-31', 20.000, 12.80, 256.00, 'SPD202609031601', '合格', NULL, NOW(), NOW()),
  (8, 7, 300001, 'AMX20260801', '2028-07-31', 30.000, 12.80, 384.00, 'SPD202609041701', '合格', NULL, NOW(), NOW()),
  (9, 8, 300001, 'AMX20260801', '2028-07-31', 10.000, 12.80, 128.00, 'SPD202609081801', '合格', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE qty = VALUES(qty), amount = VALUES(amount);

INSERT INTO trace_codes (id, spdid, code, pack_level, biz_type, status, collected_at, remark, created_at, updated_at)
VALUES
  (6, 'SPD202609031601', '01069212345678931728073110AMX20260803', '最小包装', '出库', '正常', '2026-09-03 15:05:00', NULL, NOW(), NOW()),
  (7, 'SPD202609041701', '01069212345678931728073110AMX20260804', '最小包装', '出库', '正常', '2026-09-04 11:05:00', NULL, NOW(), NOW()),
  (8, 'SPD202609081801', '01069212345678931728073110AMX20260805', '最小包装', '出库', '正常', '2026-09-08 10:45:00', NULL, NOW(), NOW()),
  (9, 'SPD202609071301', '01069212345678931728043010GC20260501', '最小包装', '出库', '正常', '2026-09-07 16:00:00', '补码演示', NOW(), NOW()),
  (10, 'SPD202609071302', '01069212345678931728080110NS20260801', '最小包装', '出库', '正常', '2026-09-07 16:01:00', '补码演示', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO batch_stocks (id, drug_id, warehouse_id, batch_no, production_date, expire_date, qty, quality_status, last_move_at, created_at, updated_at)
VALUES
  (9, 300001, 1, 'AMX20250501', '2025-05-01', '2026-04-30', 12.000, '合格', '2026-09-01 09:00:00', NOW(), NOW()),
  (10, 300003, 1, 'VC20251201', '2025-12-01', '2027-06-30', 6.000, '不合格', '2026-09-02 09:00:00', NOW(), NOW())
ON DUPLICATE KEY UPDATE qty = VALUES(qty), quality_status = VALUES(quality_status), expire_date = VALUES(expire_date);

INSERT INTO daily_close_records (id, biz_date, status, purchase_count, purchase_amount, outbound_count, outbound_amount, closed_by, closed_by_name, closed_at, remark, created_at, updated_at)
VALUES
  (1, '2026-09-01', '已日清', 1, 1250.00, 1, 180.00, 1, '超级管理员', '2026-09-01 18:00:00', '演示已日清', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status);
