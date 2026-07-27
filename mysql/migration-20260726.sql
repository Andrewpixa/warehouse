-- 2026-07-26 多仓库/库位/调拨/分仓预警 一期迁移
-- 背景：原库存模型为 bus_goods.number 全局单一库存数，无法支撑多仓管理。
-- 本脚本：
--   1. 新建 bus_warehouse / bus_warehouse_location / bus_goods_stock /
--      bus_transfer / bus_transfer_item / bus_warehouse_warn_rule 六张表
--   2. 插入默认仓"主仓库"(id=1)，将全部现有库存回填到默认仓
--   3. bus_inport/bus_sales/bus_retail/bus_stocktake/bus_serial_number 加仓库/库位列并回填默认仓
--   4. 补菜单（仓储管理/仓库管理/库存调拨/分仓库存）与操作级权限码(id 194-211)及角色授权
-- 说明：一期库存数量按"仓"粒度管理（bus_goods_stock 唯一键 goodsid+warehouse_id），
--   库位作为单据上的拣货指引记录，不参与数量扣减；bus_goods.number 保留为总库存缓存。
-- 权限/菜单部分幂等（按 id/percode 与 rid/pid 防重），DDL 部分仅执行一次。

-- 1. 仓库表
CREATE TABLE IF NOT EXISTS `bus_warehouse` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '仓库名称',
  `code` varchar(50) DEFAULT NULL COMMENT '仓库编码',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `manager` varchar(50) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(50) DEFAULT NULL COMMENT '联系电话',
  `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '是否默认仓 0=否 1=是（全系统唯一）',
  `available` int NOT NULL DEFAULT 1 COMMENT '状态 0=停用 1=启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_warehouse_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仓库表';

-- 2. 库位表（两级：仓库→库位，zone 字段做库区分组）
CREATE TABLE IF NOT EXISTS `bus_warehouse_location` (
  `id` int NOT NULL AUTO_INCREMENT,
  `warehouse_id` int NOT NULL COMMENT '所属仓库ID',
  `code` varchar(50) NOT NULL COMMENT '库位编码 如 A-01-03',
  `zone` varchar(50) DEFAULT NULL COMMENT '库区（拣货区/存储区/退货区/不良品区）',
  `name` varchar(100) DEFAULT NULL COMMENT '库位名称',
  `available` int NOT NULL DEFAULT 1 COMMENT '状态 0=停用 1=启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wh_loc_code` (`warehouse_id`,`code`),
  KEY `idx_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库位表';

-- 3. 分仓库存表（一期数量按仓粒度；bus_goods.number 为各仓之和的总库存缓存）
CREATE TABLE IF NOT EXISTS `bus_goods_stock` (
  `id` int NOT NULL AUTO_INCREMENT,
  `goodsid` int NOT NULL COMMENT '商品ID',
  `warehouse_id` int NOT NULL COMMENT '仓库ID',
  `number` int NOT NULL DEFAULT 0 COMMENT '该仓库存数量',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_goods_wh` (`goodsid`,`warehouse_id`),
  KEY `idx_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分仓库存表';

-- 4. 调拨单
CREATE TABLE IF NOT EXISTS `bus_transfer` (
  `id` int NOT NULL AUTO_INCREMENT,
  `transfer_no` varchar(50) NOT NULL COMMENT '调拨单号',
  `from_warehouse_id` int NOT NULL COMMENT '调出仓库ID',
  `to_warehouse_id` int NOT NULL COMMENT '调入仓库ID',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态 0=草稿 1=在途 2=已完成 3=已取消',
  `operator` varchar(50) DEFAULT NULL COMMENT '操作人',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `ship_time` datetime DEFAULT NULL COMMENT '发出时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transfer_no` (`transfer_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存调拨单';

CREATE TABLE IF NOT EXISTS `bus_transfer_item` (
  `id` int NOT NULL AUTO_INCREMENT,
  `transfer_id` int NOT NULL COMMENT '调拨单ID',
  `goodsid` int NOT NULL COMMENT '商品ID',
  `number` int NOT NULL COMMENT '调拨数量',
  `from_location_id` int DEFAULT NULL COMMENT '调出库位ID（拣货指引）',
  `to_location_id` int DEFAULT NULL COMMENT '调入库位ID',
  PRIMARY KEY (`id`),
  KEY `idx_transfer` (`transfer_id`),
  KEY `idx_goods` (`goodsid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调拨单明细';

-- 5. 分仓预警规则表（有规则才做分仓预警；无规则的商品仍走商品级总量预警）
CREATE TABLE IF NOT EXISTS `bus_warehouse_warn_rule` (
  `id` int NOT NULL AUTO_INCREMENT,
  `goodsid` int NOT NULL COMMENT '商品ID',
  `warehouse_id` int NOT NULL COMMENT '仓库ID',
  `dangernum` int NOT NULL COMMENT '该仓预警阈值',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_goods_wh_rule` (`goodsid`,`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分仓预警规则表';

-- 6. 默认仓 + 现有库存回填（幂等）
INSERT INTO bus_warehouse (id, name, code, is_default, available, remark, create_time)
SELECT 1, '主仓库', 'WH001', 1, 1, '系统默认仓库（迁移自动创建）', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bus_warehouse WHERE id = 1);

INSERT INTO bus_goods_stock (goodsid, warehouse_id, number)
SELECT g.id, 1, IFNULL(g.number, 0) FROM bus_goods g
WHERE NOT EXISTS (SELECT 1 FROM bus_goods_stock s WHERE s.goodsid = g.id AND s.warehouse_id = 1);

-- 7. 单据/盘点/序列表加仓库、库位列
ALTER TABLE `bus_inport`
  ADD COLUMN `warehouse_id` int DEFAULT NULL COMMENT '仓库ID',
  ADD COLUMN `location_id` int DEFAULT NULL COMMENT '库位ID（拣货指引）';
ALTER TABLE `bus_sales`
  ADD COLUMN `warehouse_id` int DEFAULT NULL COMMENT '仓库ID',
  ADD COLUMN `location_id` int DEFAULT NULL COMMENT '库位ID（拣货指引）';
ALTER TABLE `bus_retail`
  ADD COLUMN `warehouse_id` int DEFAULT NULL COMMENT '仓库ID',
  ADD COLUMN `location_id` int DEFAULT NULL COMMENT '库位ID（拣货指引）';
ALTER TABLE `bus_stocktake`
  ADD COLUMN `warehouse_id` int DEFAULT NULL COMMENT '盘点仓库ID';
ALTER TABLE `bus_serial_number`
  ADD COLUMN `warehouse_id` int DEFAULT NULL COMMENT '所在仓库ID';

-- 8. 历史数据回填默认仓
UPDATE bus_inport SET warehouse_id = 1 WHERE warehouse_id IS NULL;
UPDATE bus_sales SET warehouse_id = 1 WHERE warehouse_id IS NULL;
UPDATE bus_retail SET warehouse_id = 1 WHERE warehouse_id IS NULL;
UPDATE bus_stocktake SET warehouse_id = 1 WHERE warehouse_id IS NULL;
UPDATE bus_serial_number SET warehouse_id = 1 WHERE warehouse_id IS NULL;

-- 9. 菜单与权限码（id 194-211，幂等）
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, `open`, ordernum, available)
SELECT v.id, v.pid, v.type, v.title, v.percode, v.icon, v.href, v.target, v.`open`, v.ordernum, v.available FROM (
    SELECT 194 AS id, 1 AS pid, 'menu' AS type, '仓储管理' AS title, NULL AS percode, 'House' AS icon, '' AS href, NULL AS target, 0 AS `open`, 9 AS ordernum, 1 AS available
    UNION ALL SELECT 195, 194, 'menu', '仓库管理', NULL, 'OfficeBuilding', '/business/warehouse', NULL, 0, 1, 1
    UNION ALL SELECT 196, 195, 'permission', '仓库查询', 'warehouse:view', NULL, NULL, NULL, 0, 1, 1
    UNION ALL SELECT 197, 195, 'permission', '仓库添加', 'warehouse:create', NULL, NULL, NULL, 0, 2, 1
    UNION ALL SELECT 198, 195, 'permission', '仓库修改', 'warehouse:update', NULL, NULL, NULL, 0, 3, 1
    UNION ALL SELECT 199, 195, 'permission', '仓库删除', 'warehouse:delete', NULL, NULL, NULL, 0, 4, 1
    UNION ALL SELECT 200, 195, 'permission', '库位查询', 'location:view', NULL, NULL, NULL, 0, 5, 1
    UNION ALL SELECT 201, 195, 'permission', '库位添加', 'location:create', NULL, NULL, NULL, 0, 6, 1
    UNION ALL SELECT 202, 195, 'permission', '库位修改', 'location:update', NULL, NULL, NULL, 0, 7, 1
    UNION ALL SELECT 203, 195, 'permission', '库位删除', 'location:delete', NULL, NULL, NULL, 0, 8, 1
    UNION ALL SELECT 204, 194, 'menu', '库存调拨', NULL, 'Van', '/business/transfer', NULL, 0, 2, 1
    UNION ALL SELECT 205, 204, 'permission', '调拨查询', 'transfer:view', NULL, NULL, NULL, 0, 1, 1
    UNION ALL SELECT 206, 204, 'permission', '调拨开单', 'transfer:create', NULL, NULL, NULL, 0, 2, 1
    UNION ALL SELECT 207, 204, 'permission', '调拨发出', 'transfer:ship', NULL, NULL, NULL, 0, 3, 1
    UNION ALL SELECT 208, 204, 'permission', '调拨收货', 'transfer:receive', NULL, NULL, NULL, 0, 4, 1
    UNION ALL SELECT 209, 204, 'permission', '调拨取消', 'transfer:cancel', NULL, NULL, NULL, 0, 5, 1
    UNION ALL SELECT 210, 204, 'permission', '调拨删除', 'transfer:delete', NULL, NULL, NULL, 0, 6, 1
    UNION ALL SELECT 211, 194, 'menu', '分仓库存', NULL, 'Box', '/business/goods-stock', NULL, 0, 3, 1
) v
LEFT JOIN sys_permission p ON p.id = v.id OR (v.percode IS NOT NULL AND p.percode = v.percode)
WHERE p.id IS NULL;

-- 10. 角色授权（rid/pid 防重）
--     角色: 1超级管理员 11仓库管理员 12采购员 13销售员
INSERT INTO sys_role_permission (rid, pid)
SELECT v.rid, v.pid FROM (
    -- 超级管理员：新菜单 + 全部新权限码
    SELECT 1 AS rid, 194 AS pid
    UNION ALL SELECT 1, 195 UNION ALL SELECT 1, 204 UNION ALL SELECT 1, 211
    UNION ALL SELECT 1, 196 UNION ALL SELECT 1, 197 UNION ALL SELECT 1, 198 UNION ALL SELECT 1, 199
    UNION ALL SELECT 1, 200 UNION ALL SELECT 1, 201 UNION ALL SELECT 1, 202 UNION ALL SELECT 1, 203
    UNION ALL SELECT 1, 205 UNION ALL SELECT 1, 206 UNION ALL SELECT 1, 207
    UNION ALL SELECT 1, 208 UNION ALL SELECT 1, 209 UNION ALL SELECT 1, 210
    -- 仓库管理员：仓储全部菜单与操作
    UNION ALL SELECT 11, 194 UNION ALL SELECT 11, 195 UNION ALL SELECT 11, 204 UNION ALL SELECT 11, 211
    UNION ALL SELECT 11, 196 UNION ALL SELECT 11, 197 UNION ALL SELECT 11, 198 UNION ALL SELECT 11, 199
    UNION ALL SELECT 11, 200 UNION ALL SELECT 11, 201 UNION ALL SELECT 11, 202 UNION ALL SELECT 11, 203
    UNION ALL SELECT 11, 205 UNION ALL SELECT 11, 206 UNION ALL SELECT 11, 207
    UNION ALL SELECT 11, 208 UNION ALL SELECT 11, 209 UNION ALL SELECT 11, 210
    -- 采购员/销售员：仓库查询（开单选仓必需）
    UNION ALL SELECT 12, 196
    UNION ALL SELECT 13, 196
) v
LEFT JOIN sys_role_permission rp ON rp.rid = v.rid AND rp.pid = v.pid
WHERE rp.rid IS NULL;
