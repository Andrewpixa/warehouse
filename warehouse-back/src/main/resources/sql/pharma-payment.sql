-- 出库单回款标记（员工端按发票查询 / 未回款列表）
SET NAMES utf8mb4;

SET @col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders' AND COLUMN_NAME = 'paid_status'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE sales_orders
     ADD COLUMN paid_status VARCHAR(16) DEFAULT ''未回款'' COMMENT ''未回款/部分回款/已回款'' AFTER total_amount,
     ADD COLUMN paid_amount DECIMAL(18,2) DEFAULT 0 COMMENT ''已回金额'' AFTER paid_status,
     ADD COLUMN paid_at DATETIME NULL COMMENT ''最近回款时间'' AFTER paid_amount',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sales_orders
SET paid_status = '未回款', paid_amount = 0, paid_at = NULL
WHERE paid_status IS NULL OR paid_status = '';

-- 演示：南方医院未回款、大参林已回款、社区卫生中心部分回款
UPDATE sales_orders
SET paid_status = '未回款', paid_amount = 0, paid_at = NULL
WHERE id = 2;

UPDATE sales_orders
SET paid_status = '已回款', paid_amount = total_amount, paid_at = '2026-09-07 16:00:00'
WHERE id = 3;

INSERT INTO sales_orders (id, order_no, invoice_no, customer_id, warehouse_id, salesman_id, reviewer_id, biz_date, pay_type, status, total_amount, paid_status, paid_amount, paid_at, created_by, confirmed_by, confirmed_at, remark, created_at, updated_at)
VALUES
  (4, 'CK20260907003', '20260907000100000003', 100003, 1, 4, 5, '2026-09-07', '月结', '已确认', 134.00, '部分回款', 50.00, '2026-09-07 15:20:00', 1, 1, '2026-09-07 15:00:00', '阳光社区卫生中心部分回款演示', NOW(), NOW())
ON DUPLICATE KEY UPDATE paid_status = VALUES(paid_status), paid_amount = VALUES(paid_amount), paid_at = VALUES(paid_at), total_amount = VALUES(total_amount);

INSERT INTO sales_order_items (id, order_id, drug_id, batch_no, expire_date, qty, sale_price, amount, spdid, quality_status, remark, created_at, updated_at)
VALUES
  (4, 4, 300005, 'GC20260501', '2028-04-30', 10.000, 7.20, 72.00, 'SPD202609071301', '合格', NULL, NOW(), NOW()),
  (5, 4, 300006, 'NS20260801', '2028-08-01', 10.000, 6.20, 62.00, 'SPD202609071302', '合格', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE qty = VALUES(qty), amount = VALUES(amount);
