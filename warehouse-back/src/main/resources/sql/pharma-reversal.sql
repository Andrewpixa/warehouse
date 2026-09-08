-- 红冲单：独立出库单，确认后加回批号库存
SET NAMES utf8mb4;

SET @col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_orders' AND COLUMN_NAME = 'order_type'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE sales_orders
     ADD COLUMN order_type VARCHAR(16) DEFAULT ''正常'' COMMENT ''正常/红冲'' AFTER invoice_no,
     ADD COLUMN original_invoice_no VARCHAR(20) NULL COMMENT ''红冲对应的原蓝字发票号'' AFTER order_type',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sales_orders
SET order_type = '正常'
WHERE order_type IS NULL OR order_type = '';

-- 演示：对南方医院蓝字发票冲 10 盒阿莫西林
INSERT INTO sales_orders (id, order_no, invoice_no, order_type, original_invoice_no, customer_id, warehouse_id, salesman_id, reviewer_id, biz_date, pay_type, status, total_amount, paid_status, paid_amount, paid_at, created_by, confirmed_by, confirmed_at, remark, created_at, updated_at)
VALUES
  (5, 'HC20260907001', '20260907000100000004', '红冲', '20260907000100000001', 100001, 1, 4, 5, '2026-09-07', '月结', '已确认', 128.00, '已回款', 0.00, '2026-09-07 17:00:00', 1, 1, '2026-09-07 17:00:00', '质量投诉部分红冲演示', NOW(), NOW())
ON DUPLICATE KEY UPDATE order_type = VALUES(order_type), original_invoice_no = VALUES(original_invoice_no), status = VALUES(status);

INSERT INTO sales_order_items (id, order_id, drug_id, batch_no, expire_date, qty, sale_price, amount, spdid, quality_status, remark, created_at, updated_at)
VALUES
  (6, 5, 300001, 'AMX20260801', '2028-07-31', 10.000, 12.80, 128.00, 'SPD202609071401', '合格', '红冲原出发票 20260907000100000001', NOW(), NOW())
ON DUPLICATE KEY UPDATE qty = VALUES(qty), amount = VALUES(amount);
