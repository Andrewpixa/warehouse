-- 公司运作流程图对齐：协同单据 + 菜单 + 加厚演示数据（可重复执行）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS ops_flow_docs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  doc_type VARCHAR(32) NOT NULL,
  doc_no VARCHAR(32) NOT NULL,
  biz_date DATE DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT '草稿',
  customer_id BIGINT DEFAULT NULL,
  supplier_id BIGINT DEFAULT NULL,
  drug_id BIGINT DEFAULT NULL,
  warehouse_id BIGINT DEFAULT NULL,
  related_no VARCHAR(64) DEFAULT NULL,
  amount DECIMAL(18,2) DEFAULT 0,
  qty DECIMAL(18,3) DEFAULT 0,
  title VARCHAR(200) DEFAULT NULL,
  reason VARCHAR(255) DEFAULT NULL,
  extra_json TEXT,
  created_by BIGINT DEFAULT NULL,
  confirmed_by BIGINT DEFAULT NULL,
  confirmed_at DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ops_doc_no (doc_no),
  KEY idx_ops_type_status (doc_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ops_flow_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  doc_id BIGINT NOT NULL,
  drug_id BIGINT DEFAULT NULL,
  batch_no VARCHAR(64) DEFAULT NULL,
  qty DECIMAL(18,3) DEFAULT 0,
  price DECIMAL(18,4) DEFAULT 0,
  amount DECIMAL(18,2) DEFAULT 0,
  quality_status VARCHAR(20) DEFAULT NULL,
  expire_date DATE DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ops_item_doc (doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS maker_flow_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  login_name VARCHAR(64) NOT NULL,
  password VARCHAR(64) NOT NULL,
  supplier_id BIGINT DEFAULT NULL,
  status TINYINT DEFAULT 1,
  effective_at DATETIME DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_maker_login (login_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 300, 1, 'menu', '公司运作协同', NULL, 'SetUp', '', '', 0, 8, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 300);

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 301, 300, 'menu', '缺货补货', NULL, 'Bell', '/business/stockout', '', 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 301 OR href = '/business/stockout');
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 302, 300, 'menu', '到货异常', NULL, 'WarnTriangleFilled', '/business/inbound-ex', '', 0, 2, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 302 OR href = '/business/inbound-ex');
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 303, 300, 'menu', '销退通知单', NULL, 'RefreshLeft', '/business/return-notice', '', 0, 3, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 303 OR href = '/business/return-notice');
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 304, 300, 'menu', '收款冲账', NULL, 'Wallet', '/business/offset', '', 0, 4, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 304 OR href = '/business/offset');
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 305, 300, 'menu', '分货', NULL, 'Share', '/business/allocate', '', 0, 5, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 305 OR href = '/business/allocate');
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 306, 300, 'menu', '物流联系单', NULL, 'Van', '/business/logistics', '', 0, 6, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 306 OR href = '/business/logistics');
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 307, 300, 'menu', '信誉额', NULL, 'Medal', '/business/credit', '', 0, 7, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 307 OR href = '/business/credit');
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 308, 300, 'menu', '库容统筹值', NULL, 'Histogram', '/business/quota', '', 0, 8, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 308 OR href = '/business/quota');
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 309, 300, 'menu', '出库打印包', NULL, 'Printer', '/business/print-pack', '', 0, 9, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 309 OR href = '/business/print-pack');
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 310, 300, 'menu', '厂家流向', NULL, 'Connection', '/business/maker-flow', '', 0, 10, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 310 OR href = '/business/maker-flow');

INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 311, 300, 'permission', '协同查看', 'ops:view', NULL, NULL, NULL, 0, 20, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 311 OR percode = 'ops:view');
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 312, 300, 'permission', '协同开单', 'ops:create', NULL, NULL, NULL, 0, 21, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 312 OR percode = 'ops:create');
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 313, 300, 'permission', '协同推进', 'ops:confirm', NULL, NULL, NULL, 0, 22, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 313 OR percode = 'ops:confirm');
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
SELECT 314, 300, 'permission', '协同删除', 'ops:delete', NULL, NULL, NULL, 0, 23, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 314 OR percode = 'ops:delete');

INSERT IGNORE INTO sys_role_permission (rid, pid)
SELECT r.id, p.id
FROM sys_role r
JOIN (
  SELECT 300 AS id UNION SELECT 301 UNION SELECT 302 UNION SELECT 303 UNION SELECT 304 UNION SELECT 305
  UNION SELECT 306 UNION SELECT 307 UNION SELECT 308 UNION SELECT 309 UNION SELECT 310
  UNION SELECT 311 UNION SELECT 312 UNION SELECT 313 UNION SELECT 314
) p
WHERE r.id IN (1, 11, 12, 13, 14);

-- 档案加厚
INSERT INTO suppliers (id, code, name, license_no, contact, phone, address, settle_type, first_camp_ok, status, remark, created_at, updated_at)
VALUES
  (200010, '200010', '扬子江药业集团有限公司', '苏AA1004001', '沈经理', '0514-80880001', '泰州市扬子江南路1号', '月结', 1, 1, NULL, NOW(), NOW()),
  (200011, '200011', '齐鲁制药有限公司', '鲁AA1005001', '韩经理', '0531-80880002', '济南市高新区新泺大街317号', '月结', 1, 1, NULL, NOW(), NOW()),
  (200012, '200012', '石药集团欧意药业有限公司', '冀AA1006001', '马主管', '0311-80880003', '石家庄市中山西路88号', '现结', 1, 1, NULL, NOW(), NOW()),
  (200013, '200013', '上海医药分销控股有限公司', '沪AA1007001', '丁经理', '021-80880004', '上海市静安区南京西路1486号', '月结', 1, 1, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO customers (id, code, name, customer_type, license_no, contact, phone, address, settle_type, status, remark, created_at, updated_at)
VALUES
  (100010, '100010', '中山大学附属第一医院', '医院', '粤DA1004001', '药剂科-周主任', '020-87755766', '广州市越秀区中山二路58号', '月结', 1, NULL, NOW(), NOW()),
  (100011, '100011', '广东省人民医院', '医院', '粤DA1005001', '药学部-何主任', '020-83827812', '广州市越秀区中山二路106号', '月结', 1, NULL, NOW(), NOW()),
  (100012, '100012', '海王星辰（珠江新城店）', '药店', '粤DA1006001', '李店长', '020-38881122', '广州市天河区花城大道85号', '现结', 1, NULL, NOW(), NOW()),
  (100013, '100013', '广州市红十字会医院', '医院', '粤DA1007001', '药剂科-吴主任', '020-34403800', '广州市海珠区同福中路396号', '月结', 1, NULL, NOW(), NOW()),
  (100014, '100014', '番禺区中心医院', '医院', '粤DA1008001', '药库-郑主管', '020-34833333', '广州市番禺区桥兴大道8号', '月结', 1, NULL, NOW(), NOW()),
  (100015, '100015', '益丰大药房（黄埔店）', '药店', '粤DA1009001', '陈店长', '020-82501100', '广州市黄埔区开发大道193号', '现结', 1, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO drugs (id, code, category, generic_name, trade_name, spec, dosage_form, unit, manufacturer, approval_no, barcode, is_cold_chain, rx_type, ref_purchase_price, ref_sale_price, status, remark, created_at, updated_at)
VALUES
  (300008, '300008', '药品', '阿托伐他汀钙片', '立普妥', '20mg*7片', '片剂', '盒', '辉瑞制药有限公司', '国药准字H20051408', NULL, 0, '处方药', 28.50, 42.80, 1, NULL, NOW(), NOW()),
  (300009, '300009', '药品', '奥美拉唑肠溶胶囊', '洛赛克', '20mg*14粒', '胶囊', '盒', '阿斯利康制药有限公司', '国药准字H20044871', NULL, 0, '处方药', 16.20, 24.50, 1, NULL, NOW(), NOW()),
  (300010, '300010', '药品', '蒙脱石散', '思密达', '3g*10袋', '散剂', '盒', '博福-益普生（天津）制药有限公司', '国药准字H20000690', NULL, 0, 'OTC', 12.80, 19.60, 1, NULL, NOW(), NOW()),
  (300011, '300011', '药品', '布洛芬缓释胶囊', '芬必得', '0.3g*20粒', '胶囊', '盒', '中美天津史克制药有限公司', '国药准字H20013062', NULL, 0, 'OTC', 9.40, 14.80, 1, NULL, NOW(), NOW()),
  (300012, '300012', '药品', '左氧氟沙星注射液', NULL, '0.3g*100ml', '注射液', '袋', '扬子江药业集团有限公司', '国药准字H19990324', NULL, 0, '处方药', 8.90, 13.50, 1, NULL, NOW(), NOW()),
  (300013, '300013', '药品', '注射用头孢曲松钠', NULL, '1.0g', '粉针', '支', '齐鲁制药有限公司', '国药准字H20023072', NULL, 0, '处方药', 3.60, 6.20, 1, NULL, NOW(), NOW()),
  (300014, '300014', '药品', '阿德福韦酯片', NULL, '10mg*14片', '片剂', '盒', '葛兰素史克（天津）有限公司', '国药准字H20070181', NULL, 1, '处方药', 52.00, 78.00, 1, '2～8℃', NOW(), NOW()),
  (300015, '300015', '药品', '对乙酰氨基酚片', '必理通', '0.5g*10片', '片剂', '盒', '中美天津史克制药有限公司', '国药准字H14020771', NULL, 0, 'OTC', 4.80, 8.50, 1, NULL, NOW(), NOW()),
  (300016, '300016', '药品', '氨氯地平片', '络活喜', '5mg*7片', '片剂', '盒', '辉瑞制药有限公司', '国药准字H10950224', NULL, 0, '处方药', 18.60, 27.90, 1, NULL, NOW(), NOW()),
  (400003, '400003', '器械', '一次性使用输液器', NULL, '带针', '输液器', '套', '山东威高集团医用高分子制品股份有限公司', '鲁械注准20162660011', NULL, 0, NULL, 1.10, 2.00, 1, NULL, NOW(), NOW()),
  (400004, '400004', '器械', '医用外科口罩', '稳健', '17.5×9.5cm*50只', '口罩', '盒', '稳健医疗用品股份有限公司', '粤械注准20162640008', NULL, 0, NULL, 8.50, 15.00, 1, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE generic_name = VALUES(generic_name);

INSERT INTO batch_stocks (id, drug_id, warehouse_id, batch_no, production_date, expire_date, qty, quality_status, last_move_at, created_at, updated_at)
VALUES
  (20, 300008, 1, 'LP20260801', '2026-08-01', '2028-07-31', 40.000, '合格', NOW(), NOW(), NOW()),
  (21, 300009, 1, 'OMZ20260701', '2026-07-01', '2028-06-30', 2.000, '合格', NOW(), NOW(), NOW()),
  (22, 300010, 1, 'SMT20260501', '2026-05-01', '2028-04-30', 90.000, '合格', NOW(), NOW(), NOW()),
  (23, 300011, 1, 'IBU20260401', '2026-04-01', '2026-10-15', 25.000, '合格', NOW(), NOW(), NOW()),
  (24, 300012, 1, 'LVX20260801', '2026-08-01', '2028-08-01', 120.000, '合格', NOW(), NOW(), NOW()),
  (25, 300013, 1, 'CRO20260801', '2026-08-01', '2028-07-31', 200.000, '合格', NOW(), NOW(), NOW()),
  (26, 300014, 3, 'ADV20260701', '2026-07-01', '2027-01-20', 6.000, '合格', NOW(), NOW(), NOW()),
  (27, 300015, 1, 'PCM20260101', '2026-01-01', '2026-09-30', 3.000, '合格', NOW(), NOW(), NOW()),
  (28, 300016, 1, 'AML20260801', '2026-08-01', '2028-07-31', 55.000, '合格', NOW(), NOW(), NOW()),
  (29, 400003, 1, 'IV20260801', '2026-08-01', '2029-07-31', 500.000, '合格', NOW(), NOW(), NOW()),
  (30, 300008, 2, 'LP20260801', '2026-08-01', '2028-07-31', 12.000, '待验', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE qty = VALUES(qty);

INSERT INTO ops_flow_docs (id, doc_type, doc_no, biz_date, status, customer_id, supplier_id, drug_id, warehouse_id, related_no, amount, qty, title, reason, remark, created_by, created_at, updated_at)
VALUES
  (1, 'STOCKOUT', 'QH20260915001', '2026-09-15', '已登记', 100001, NULL, 300009, 1, NULL, 0, 30, '南方医院开票缺奥美拉唑', '现货不足', '采购按下限补货', 1, NOW(), NOW()),
  (2, 'STOCKOUT', 'QH20260915002', '2026-09-15', '已补货', 100012, NULL, 300015, 1, 'CG20260915001', 0, 50, '必理通近效期+低库存', '低于下限', NULL, 1, NOW(), NOW()),
  (3, 'INBOUND_EX', 'YC20260914001', '2026-09-14', '待复检', NULL, 200010, 300012, 2, 'HT20260901', 0, 100, '扬子江左氧到货缺检验报告', '缺少资料', '入待复检，采购补资料', 1, NOW(), NOW()),
  (4, 'INBOUND_EX', 'YC20260914002', '2026-09-14', '待处理', NULL, 200011, 300013, 2, 'HT20260902', 0, 40, '齐鲁头孢外箱压损', '货品残损', '与质量沟通后判定', 1, NOW(), NOW()),
  (5, 'INBOUND_EX', 'YC20260913001', '2026-09-13', '合格', NULL, 200001, 300014, 3, 'HT20260903', 0, 20, '冷链超温有保证函', '超温', '质量部审核保证函后合格', 1, NOW(), NOW()),
  (6, 'RETURN_NOTICE', 'XT20260915001', '2026-09-15', '已释放', 100002, NULL, 300003, 4, 'CK20260907002', 227.50, 35, '大参林拒收VC再送/销退', '客户拒收', '运输类：下次送该点收回', 1, NOW(), NOW()),
  (7, 'RETURN_NOTICE', 'XT20260912001', '2026-09-12', '已入库', 100001, NULL, 300001, 4, 'CK20260907001', 128.00, 10, '南方医院滞销退10盒阿莫仙', '滞销', '不运输：业务员送回仓库', 1, NOW(), NOW()),
  (8, 'OFFSET', 'CZ20260916001', '2026-09-16', '草稿', 100001, NULL, NULL, NULL, '20260907000100000001', 512.00, 0, '南方医院公对公到账冲发票', '公对公', '冲账金额与到账一致', 1, NOW(), NOW()),
  (9, 'OFFSET', 'CZ20260916002', '2026-09-16', '草稿', 100003, NULL, NULL, NULL, '20260907000100000003', 84.00, 0, '阳光社区挂账冲账差50', '到账不一致', '需OA挂账后内务签名', 1, NOW(), NOW()),
  (10, 'ALLOCATE', 'FH20260915001', '2026-09-15', '待分货', 100011, NULL, 300012, 1, 'WD2026091508', 0, 80, '省医网单左氧需分货', '需要分货', '全药网订单', 1, NOW(), NOW()),
  (11, 'ALLOCATE', 'FH20260914001', '2026-09-14', '已分货', 100010, NULL, 300008, 1, 'WD2026091402', 0, 20, '中山一立普妥分货完成', '分货完成', '可开票', 1, NOW(), NOW()),
  (12, 'LOGISTICS', 'WL20260915001', '2026-09-15', '草稿', 100014, NULL, NULL, 1, 'CKPLAN001', 0, 120, '番禺中心超100件预约入库', '超100件', '需物流联系单预约', 1, NOW(), NOW()),
  (13, 'LOGISTICS', 'WL20260915002', '2026-09-15', '已同意', 100013, NULL, NULL, 1, NULL, 0, 0, '红会医院要求午后送达', '特殊送货', '运营经理同意后开票员写单', 1, NOW(), NOW()),
  (14, 'CREDIT', 'XY20260901001', '2026-09-01', '已生效', 100001, NULL, NULL, NULL, NULL, 200000.00, 0, '南方医院信誉额20万', '月结授信', '开票预处理校验', 1, NOW(), NOW()),
  (15, 'CREDIT', 'XY20260901002', '2026-09-01', '已生效', 100002, NULL, NULL, NULL, NULL, 30000.00, 0, '大参林现结信誉额3万', '证照齐全', NULL, 1, NOW(), NOW()),
  (16, 'QUOTA', 'TC20260915001', '2026-09-15', '草稿', NULL, NULL, NULL, 1, NULL, 0, 800, '广深惠常温库申请加统筹', '超统筹值', '采购反馈运营转物流', 1, NOW(), NOW()),
  (17, 'QUOTA', 'TC20260910001', '2026-09-10', '已生效', NULL, NULL, NULL, 3, NULL, 0, 120, '冷链仓库容统筹已增加', '库容', NULL, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), title = VALUES(title);

INSERT INTO ops_flow_items (id, doc_id, drug_id, batch_no, qty, price, amount, quality_status, expire_date, remark)
VALUES
  (1, 1, 300009, NULL, 30, 16.20, 486.00, '合格', NULL, '缺货待补'),
  (2, 6, 300003, 'VC20260101', 35, 6.50, 227.50, '待验', '2027-06-30', '拒收运回'),
  (3, 7, 300001, 'AMX20260801', 10, 12.80, 128.00, '待验', '2028-07-31', '销退入库'),
  (4, 10, 300012, 'LVX20260801', 80, 13.50, 1080.00, '合格', '2028-08-01', '待分货')
ON DUPLICATE KEY UPDATE qty = VALUES(qty);

INSERT INTO maker_flow_accounts (id, login_name, password, supplier_id, status, effective_at, remark, created_at)
VALUES
  (1, 'gzyy', '123456', 200001, 1, '2026-09-15 00:00:00', '广州医药厂家流向查询，次日生效', NOW()),
  (2, 'yzj', '123456', 200010, 1, '2026-09-16 00:00:00', '扬子江', NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO sales_orders (id, order_no, invoice_no, customer_id, warehouse_id, salesman_id, reviewer_id, biz_date, pay_type, status, total_amount, paid_status, paid_amount, created_by, confirmed_by, confirmed_at, receive_status, remark, created_at, updated_at)
VALUES
  (10, 'CK20260915001', '20260915000100000010', 100010, 1, 4, 5, '2026-09-15', '月结', '已确认', 856.00, '未回款', 0, 1, 1, '2026-09-15 10:00:00', '待收货', '中山一常规购进', NOW(), NOW()),
  (11, 'CK20260915002', '20260915000100000011', 100011, 1, 4, 5, '2026-09-15', '月结', '已确认', 1350.00, '未回款', 0, 1, 1, '2026-09-15 11:20:00', '待收货', '省医网单已分货开票', NOW(), NOW()),
  (12, 'CK20260914001', '20260914000100000012', 100013, 1, 4, 5, '2026-09-14', '月结', '已确认', 279.00, '部分回款', 100.00, 1, 1, '2026-09-14 16:00:00', '已签收', '红会医院', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), receive_status = VALUES(receive_status);

INSERT INTO sales_order_items (id, order_id, drug_id, batch_no, expire_date, qty, sale_price, amount, spdid, quality_status, remark, created_at, updated_at)
VALUES
  (20, 10, 300008, 'LP20260801', '2028-07-31', 20.000, 42.80, 856.00, 'SPD202609151001', '合格', NULL, NOW(), NOW()),
  (21, 11, 300012, 'LVX20260801', '2028-08-01', 100.000, 13.50, 1350.00, 'SPD202609151002', '合格', NULL, NOW(), NOW()),
  (22, 12, 300016, 'AML20260801', '2028-07-31', 10.000, 27.90, 279.00, 'SPD202609141001', '合格', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE qty = VALUES(qty);

INSERT INTO purchase_orders (id, order_no, invoice_no, supplier_id, warehouse_id, salesman_id, checker_id, keeper_id, biz_date, check_result, status, total_amount, created_by, confirmed_by, confirmed_at, remark, created_at, updated_at)
VALUES
  (20, 'CG20260915001', 'FP-YZJ-0915', 200010, 1, 2, 5, 3, '2026-09-15', '合格', '已确认', 1068.00, 1, 1, '2026-09-15 09:30:00', '按缺货补货左氧', NOW(), NOW()),
  (21, 'CG20260914001', 'FP-QL-0914', 200011, 2, 2, NULL, 3, '2026-09-14', NULL, '草稿', 0.00, 1, NULL, NULL, '残损待质量判定不得确认', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO purchase_order_items (id, order_id, drug_id, batch_no, production_date, expire_date, receive_qty, qualified_qty, stock_in_qty, purchase_price, amount, quality_status, spdid, remark, created_at, updated_at)
VALUES
  (30, 20, 300012, 'LVX20260801', '2026-08-01', '2028-08-01', 120.000, 120.000, 120.000, 8.90, 1068.00, '合格', 'SPD202609150201', NULL, NOW(), NOW()),
  (31, 21, 300013, 'CRO20260801', '2026-08-01', '2028-07-31', 40.000, 0.000, 0.000, 3.60, 144.00, '待验', 'SPD202609140201', '外箱压损', NOW(), NOW())
ON DUPLICATE KEY UPDATE stock_in_qty = VALUES(stock_in_qty);

INSERT INTO trace_codes (id, spdid, code, pack_level, biz_type, status, collected_at, remark, created_at, updated_at)
VALUES
  (20, 'SPD202609151001', '81000000000000000021', '最小包装', '出库', '正常', '2026-09-15 10:05:00', NULL, NOW(), NOW()),
  (21, 'SPD202609151001', '81000000000000000022', '最小包装', '出库', '正常', '2026-09-15 10:05:00', NULL, NOW(), NOW()),
  (22, 'SPD202609151002', '81000000000000000023', '最小包装', '出库', '正常', '2026-09-15 11:22:00', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status);
