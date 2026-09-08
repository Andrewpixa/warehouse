-- 药企演示数据 + 隐藏旧仓管菜单
-- 档案编号：客户 1xxxxx / 供应商 2xxxxx / 药品 3xxxxx / 器械 4xxxxx
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 品种类型列（3=药品 4=器械）
SET @col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'drugs' AND COLUMN_NAME = 'category'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE drugs ADD COLUMN category VARCHAR(20) DEFAULT ''药品'' COMMENT ''品种类型：药品/器械'' AFTER code',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 把旧演示主键改成 6 位规则（已改过的库会跳过）
UPDATE sales_order_items SET drug_id = drug_id + 299998 WHERE drug_id BETWEEN 3 AND 8 AND drug_id < 100000;
UPDATE purchase_order_items SET drug_id = drug_id + 299998 WHERE drug_id BETWEEN 3 AND 8 AND drug_id < 100000;
UPDATE batch_stocks SET drug_id = drug_id + 299998 WHERE drug_id BETWEEN 3 AND 8 AND drug_id < 100000;
UPDATE drugs SET id = id + 299998 WHERE id BETWEEN 3 AND 8 AND id < 100000;
UPDATE drugs SET category = '药品', code = CAST(id AS CHAR) WHERE id BETWEEN 300001 AND 399999;

UPDATE sales_order_items SET drug_id = 300007 WHERE drug_id = 2;
UPDATE purchase_order_items SET drug_id = 300007 WHERE drug_id = 2;
UPDATE batch_stocks SET drug_id = 300007 WHERE drug_id = 2;
UPDATE drugs SET id = 300007, code = '300007', category = '药品' WHERE id = 2;

UPDATE sales_orders SET customer_id = customer_id + 99998 WHERE customer_id BETWEEN 3 AND 5 AND customer_id < 100000;
UPDATE sales_orders SET customer_id = 100004 WHERE customer_id = 2;
UPDATE customers SET id = id + 99998 WHERE id BETWEEN 3 AND 5 AND id < 100000;
UPDATE customers SET id = 100004, code = '100004' WHERE id = 2;
UPDATE customers SET code = CAST(id AS CHAR) WHERE id BETWEEN 100001 AND 199999;

UPDATE purchase_orders SET supplier_id = supplier_id + 199998 WHERE supplier_id BETWEEN 3 AND 5 AND supplier_id < 100000;
UPDATE purchase_orders SET supplier_id = 200004 WHERE supplier_id = 2;
UPDATE suppliers SET id = id + 199998 WHERE id BETWEEN 3 AND 5 AND id < 100000;
UPDATE suppliers SET id = 200004, code = '200004' WHERE id = 2;
UPDATE suppliers SET code = CAST(id AS CHAR) WHERE id BETWEEN 200001 AND 299999;

SET FOREIGN_KEY_CHECKS = 1;

-- 1) 菜单：药企用语，并关掉会打旧表的入口
UPDATE sys_permission SET title = '药品进销存' WHERE id = 1;
UPDATE sys_permission SET title = '基础档案' WHERE id = 151;
UPDATE sys_permission SET title = '采购入库', icon = 'Download' WHERE id = 3;
UPDATE sys_permission SET title = '销售出库', icon = 'Sell' WHERE id = 4;
UPDATE sys_permission SET title = '仓储质量' WHERE id = 194;
UPDATE sys_permission SET title = '采购入库单' WHERE id = 10;
UPDATE sys_permission SET title = '销售出库单' WHERE id = 12;
UPDATE sys_permission SET title = '品种管理' WHERE title = '药品管理' OR href IN ('/business/drug', 'business/drug');

UPDATE sys_permission SET available = 0 WHERE id IN (
  11, 13,
  125, 126, 127, 128,
  130, 133, 134, 135, 136, 141, 145, 148, 150, 152, 153,
  155, 156, 157, 158, 159, 160, 161, 166, 167,
  188, 204
);

-- 2) 仓库（地点仓名 + GSP 类型）
INSERT INTO warehouses (id, code, name, wh_type, address, manager_id, status, remark, created_at, updated_at)
VALUES
  (1, 'WH-GSH', '广深惠仓库', '合格', '广州市黄埔区广深沿江物流园A栋', 1, 1, '广深惠主仓，常温合格品', NOW(), NOW()),
  (2, 'WH-BY', '白云仓', '待验', '广州市白云区太和收货待验区', 1, 1, '到货待验', NOW(), NOW()),
  (3, 'WH-HP-COLD', '黄埔冷链仓', '合格', '广州市黄埔区科学城冷链仓A区', 1, 1, '2～8℃冷藏药品', NOW(), NOW()),
  (4, 'WH-HP-RET', '科学城退货仓', '退货', '广州市黄埔区科学城退货暂存区', 1, 1, '销后退回待验', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), code = VALUES(code), wh_type = VALUES(wh_type), address = VALUES(address), remark = VALUES(remark);

-- 3) 员工
INSERT INTO employees (id, emp_no, name, position_id, password, mobile, hire_date, status, remark, created_at, updated_at)
VALUES
  (2, 'P001', '李采购', 1, '123456', '13800001001', '2024-03-01', 1, NULL, NOW(), NOW()),
  (3, 'K001', '王保管', 2, '123456', '13800001002', '2023-08-15', 1, NULL, NOW(), NOW()),
  (4, 'Y001', '陈业务', 3, '123456', '13800001003', '2024-01-10', 1, NULL, NOW(), NOW()),
  (5, 'Q001', '赵质管', 4, '123456', '13800001004', '2022-11-20', 1, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), position_id = VALUES(position_id);

-- 4) 供应商
INSERT INTO suppliers (id, code, name, license_no, contact, phone, address, settle_type, first_camp_ok, status, remark, created_at, updated_at)
VALUES
  (200001, '200001', '广州医药股份有限公司', '粤AA1234567', '刘经理', '020-88880001', '广州市荔湾区一德路168号', '月结', 1, 1, '集团主供应商', NOW(), NOW()),
  (200002, '200002', '国药控股广州有限公司', '粤AA7654321', '周主管', '020-88880002', '广州市天河区黄埔大道中99号', '月结', 1, 1, NULL, NOW(), NOW()),
  (200003, '200003', '华润广东医药有限公司', '粤AA5566778', '吴经理', '020-88880003', '广州市海珠区新港东路2400号', '现结', 0, 1, '首营资料审核中', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), first_camp_ok = VALUES(first_camp_ok);

-- 5) 下游客户
INSERT INTO customers (id, code, name, customer_type, license_no, contact, phone, address, settle_type, status, remark, created_at, updated_at)
VALUES
  (100001, '100001', '南方医科大学南方医院', '医院', '粤DA0011223', '药剂科-林主任', '020-61641000', '广州市白云区广州大道北1838号', '月结', 1, NULL, NOW(), NOW()),
  (100002, '100002', '大参林医药集团（天河店）', '药店', '粤DA4455667', '张店长', '020-38880011', '广州市天河区天河路208号', '现结', 1, NULL, NOW(), NOW()),
  (100003, '100003', '天河区阳光社区卫生服务中心', '诊所', '粤DA8899001', '黄医生', '020-38880022', '广州市天河区员村二横路12号', '月结', 1, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), customer_type = VALUES(customer_type);

-- 6) 药品档案
INSERT INTO drugs (id, code, category, generic_name, trade_name, spec, dosage_form, unit, manufacturer, approval_no, barcode, is_cold_chain, rx_type, ref_purchase_price, ref_sale_price, status, remark, created_at, updated_at)
VALUES
  (300001, '300001', '药品', '阿莫西林胶囊', '阿莫仙', '0.25g*24粒', '胶囊', '盒', '联邦制药厂有限公司', '国药准字H44021351', NULL, 0, '处方药', 8.60, 12.80, 1, NULL, NOW(), NOW()),
  (300002, '300002', '药品', '重组人胰岛素注射液', '诺和灵R', '10ml:400IU', '注射液', '支', '诺和诺德（中国）制药有限公司', '国药准字J20171005', NULL, 1, '处方药', 46.00, 68.00, 1, '2～8℃冷藏', NOW(), NOW()),
  (300003, '300003', '药品', '维生素C片', '果味VC', '0.1g*100片', '片剂', '瓶', '华中药业股份有限公司', '国药准字H42020610', NULL, 0, 'OTC', 3.20, 6.50, 1, NULL, NOW(), NOW()),
  (300004, '300004', '药品', '头孢克肟分散片', '世福素', '0.1g*6片', '片剂', '盒', '广州白云山制药股份有限公司', '国药准字H20041732', NULL, 0, '处方药', 18.50, 26.80, 1, NULL, NOW(), NOW()),
  (300005, '300005', '药品', '复方甘草片', NULL, '50mg*100片', '片剂', '瓶', '国药集团工业有限公司', '国药准字H21021322', NULL, 0, 'OTC', 4.10, 7.20, 1, NULL, NOW(), NOW()),
  (300006, '300006', '药品', '氯化钠注射液', '生理盐水', '250ml:2.25g', '注射液', '袋', '四川科伦药业股份有限公司', '国药准字H51021158', NULL, 0, '处方药', 2.10, 3.60, 1, NULL, NOW(), NOW()),
  (400001, '400001', '器械', '一次性使用无菌注射器', NULL, '2ml', '注射器', '支', '山东威高集团医用高分子制品股份有限公司', '粤械注准20172140001', NULL, 0, NULL, 0.80, 1.50, 1, NULL, NOW(), NOW()),
  (400002, '400002', '器械', '医用外科口罩', NULL, '17.5cm×9.5cm', '口罩', '只', '稳健医疗用品股份有限公司', '粤械注准20162640002', NULL, 0, NULL, 0.35, 0.80, 1, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE generic_name = VALUES(generic_name), trade_name = VALUES(trade_name), category = VALUES(category);

-- 7) 采购入库单
INSERT INTO purchase_orders (id, order_no, supplier_id, warehouse_id, salesman_id, checker_id, keeper_id, biz_date, check_result, status, total_amount, created_by, confirmed_by, confirmed_at, remark, created_at, updated_at)
VALUES
  (2, 'CG20260907001', 200001, 1, 2, 5, 3, '2026-09-05', '合格', '已确认', 4120.00, 1, 1, '2026-09-05 10:20:00', '常温品种采购入库', NOW(), NOW()),
  (3, 'CG20260907002', 200002, 3, 2, 5, 3, '2026-09-06', '合格', '已确认', 2300.00, 1, 1, '2026-09-06 09:15:00', '冷链胰岛素专车入冷链库', NOW(), NOW()),
  (4, 'CG20260907003', 200001, 1, 2, NULL, NULL, '2026-09-07', NULL, '草稿', 0.00, 1, NULL, NULL, '待验收草稿', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), total_amount = VALUES(total_amount);

INSERT INTO purchase_order_items (id, order_id, drug_id, batch_no, production_date, expire_date, receive_qty, qualified_qty, stock_in_qty, purchase_price, amount, quality_status, spdid, remark, created_at, updated_at)
VALUES
  (2, 2, 300001, 'AMX20260801', '2026-08-01', '2028-07-31', 200.000, 200.000, 200.000, 8.60, 1720.00, '合格', 'SPD20260907001', NULL, NOW(), NOW()),
  (3, 2, 300003, 'VC20260101', '2026-01-01', '2027-06-30', 40.000, 40.000, 40.000, 3.20, 128.00, '合格', 'SPD20260907002', NULL, NOW(), NOW()),
  (4, 2, 300004, 'CFX20260301', '2026-03-01', '2026-10-20', 80.000, 80.000, 80.000, 18.50, 1480.00, '合格', 'SPD20260907003', '近效期品种', NOW(), NOW()),
  (5, 2, 300005, 'GC20260501', '2026-05-01', '2028-04-30', 150.000, 150.000, 150.000, 4.10, 615.00, '合格', 'SPD20260907004', NULL, NOW(), NOW()),
  (6, 2, 300006, 'NS20260801', '2026-08-01', '2028-08-01', 80.000, 80.000, 80.000, 2.10, 168.00, '合格', 'SPD20260907005', NULL, NOW(), NOW()),
  (7, 3, 300002, 'INS20260701', '2026-07-01', '2027-01-15', 50.000, 50.000, 50.000, 46.00, 2300.00, '合格', 'SPD20260907006', '冷链到货', NOW(), NOW())
ON DUPLICATE KEY UPDATE stock_in_qty = VALUES(stock_in_qty);

-- 8) 销售出库单（发票号 20 位：12 位代码=业务日期+0001，后 8 位数据流水）
UPDATE sales_orders SET invoice_no = '20260901000100000001' WHERE id = 1;
UPDATE sales_orders SET invoice_no = '20260907000100000001' WHERE id = 2;
UPDATE sales_orders SET invoice_no = '20260907000100000002' WHERE id = 3;
UPDATE sales_orders SET invoice_no = CONCAT(DATE_FORMAT(biz_date, '%Y%m%d'), '0001', LPAD(id, 8, '0'))
WHERE invoice_no LIKE 'FP%' OR (invoice_no IS NOT NULL AND CHAR_LENGTH(invoice_no) <> 20);

INSERT INTO sales_orders (id, order_no, invoice_no, customer_id, warehouse_id, salesman_id, reviewer_id, biz_date, pay_type, status, total_amount, created_by, confirmed_by, confirmed_at, remark, created_at, updated_at)
VALUES
  (2, 'CK20260907001', '20260907000100000001', 100001, 1, 4, 5, '2026-09-07', '月结', '已确认', 512.00, 1, 1, '2026-09-07 11:30:00', '南方医院常规购进', NOW(), NOW()),
  (3, 'CK20260907002', '20260907000100000002', 100002, 1, 4, 5, '2026-09-07', '现结', '已确认', 227.50, 1, 1, '2026-09-07 14:10:00', '大参林零售补货', NOW(), NOW())
ON DUPLICATE KEY UPDATE invoice_no = VALUES(invoice_no), status = VALUES(status);

INSERT INTO sales_order_items (id, order_id, drug_id, batch_no, expire_date, qty, sale_price, amount, spdid, quality_status, remark, created_at, updated_at)
VALUES
  (2, 2, 300001, 'AMX20260801', '2028-07-31', 40.000, 12.80, 512.00, 'SPD202609071101', '合格', NULL, NOW(), NOW()),
  (3, 3, 300003, 'VC20260101', '2027-06-30', 35.000, 6.50, 227.50, 'SPD202609071201', '合格', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE qty = VALUES(qty);

-- 9) 批号库存（采购入 - 销售出；含低库存、近效期）
INSERT INTO batch_stocks (id, drug_id, warehouse_id, batch_no, production_date, expire_date, qty, quality_status, last_move_at, created_at, updated_at)
VALUES
  (2, 300001, 1, 'AMX20260801', '2026-08-01', '2028-07-31', 160.000, '合格', '2026-09-07 11:30:00', NOW(), NOW()),
  (3, 300002, 3, 'INS20260701', '2026-07-01', '2027-01-15', 8.000, '合格', '2026-09-06 09:15:00', NOW(), NOW()),
  (4, 300003, 1, 'VC20260101', '2026-01-01', '2027-06-30', 5.000, '合格', '2026-09-07 14:10:00', NOW(), NOW()),
  (5, 300004, 1, 'CFX20260301', '2026-03-01', '2026-10-20', 80.000, '合格', '2026-09-05 10:20:00', NOW(), NOW()),
  (6, 300005, 1, 'GC20260501', '2026-05-01', '2028-04-30', 150.000, '合格', '2026-09-05 10:20:00', NOW(), NOW()),
  (7, 300006, 1, 'NS20260801', '2026-08-01', '2028-08-01', 80.000, '合格', '2026-09-05 10:20:00', NOW(), NOW()),
  (8, 400001, 1, 'SYR20260801', '2026-08-01', '2029-07-31', 200.000, '合格', '2026-09-05 10:20:00', NOW(), NOW())
ON DUPLICATE KEY UPDATE qty = VALUES(qty), expire_date = VALUES(expire_date);

-- 10) 追溯码（发票 20260907000100000001 / 20260907000100000002）
INSERT INTO trace_codes (id, spdid, code, pack_level, biz_type, status, collected_at, remark, created_at, updated_at)
VALUES
  (3, 'SPD202609071101', '01069212345678931728073110AMX20260801', '最小包装', '出库', '正常', '2026-09-07 11:32:00', NULL, NOW(), NOW()),
  (4, 'SPD202609071101', '01069212345678931728073110AMX20260802', '最小包装', '出库', '正常', '2026-09-07 11:32:00', NULL, NOW(), NOW()),
  (5, 'SPD202609071201', '01069212345678001727063010VC20260101', '最小包装', '出库', '正常', '2026-09-07 14:12:00', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- 11) 批号升益表 + 菜单（可重复执行，详见 pharma-surplus.sql）
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

-- 12) 日结检查菜单（可重复执行，详见 pharma-daily-close.sql）
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

-- 13) 日清月结 / 进货出货统计：见 pharma-close-stats.sql
