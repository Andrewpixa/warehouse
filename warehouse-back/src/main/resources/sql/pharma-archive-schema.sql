-- ================================================================
-- 业务数据归档表 DDL
-- 库:pharma_ims
-- 说明:镜像 8 张业务表的字段 + 追加 archived_at 字段,完全无外键约束
--      归档表主键保留原 id(原表已 DELETE,不会冲突)
-- 可重复执行:CREATE TABLE IF NOT EXISTS
-- ================================================================

-- 1. 盘点单主表
CREATE TABLE IF NOT EXISTS `archive_bus_stocktake` (
  `id` int NOT NULL,
  `stocktake_no` varchar(50) DEFAULT NULL,
  `status` int DEFAULT NULL,
  `operator` varchar(50) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `warehouse_id` int DEFAULT NULL COMMENT '盘点仓库ID',
  `archived_at` datetime NOT NULL COMMENT '归档时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='盘点单归档表';

-- 2. 盘点明细
CREATE TABLE IF NOT EXISTS `archive_bus_stocktake_item` (
  `id` int NOT NULL,
  `stocktake_id` int DEFAULT NULL,
  `goodsid` int DEFAULT NULL,
  `system_num` int DEFAULT NULL,
  `actual_num` int DEFAULT NULL,
  `diff_num` int DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `archived_at` datetime NOT NULL COMMENT '归档时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_stocktake_archived` (`stocktake_id`, `archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='盘点明细归档表';

-- 3. 采购单主表(purchase_orders)
CREATE TABLE IF NOT EXISTS `archive_purchase_orders` (
  `id` bigint NOT NULL,
  `order_no` varchar(64) DEFAULT NULL,
  `invoice_no` varchar(128) DEFAULT NULL,
  `supplier_id` bigint DEFAULT NULL,
  `warehouse_id` bigint DEFAULT NULL,
  `salesman_id` bigint DEFAULT NULL,
  `checker_id` bigint DEFAULT NULL,
  `keeper_id` bigint DEFAULT NULL,
  `biz_date` date DEFAULT NULL,
  `check_result` varchar(255) DEFAULT NULL,
  `status` varchar(32) DEFAULT NULL COMMENT '草稿 / 已确认 / 已过账',
  `total_amount` decimal(18,2) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `confirmed_by` bigint DEFAULT NULL,
  `confirmed_at` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `archived_at` datetime NOT NULL COMMENT '归档时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购单归档表';

-- 4. 采购单明细(purchase_order_items)
CREATE TABLE IF NOT EXISTS `archive_purchase_order_items` (
  `id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `drug_id` bigint DEFAULT NULL,
  `batch_no` varchar(64) DEFAULT NULL,
  `production_date` date DEFAULT NULL,
  `expire_date` date DEFAULT NULL,
  `receive_qty` decimal(18,2) DEFAULT NULL,
  `qualified_qty` decimal(18,2) DEFAULT NULL,
  `stock_in_qty` decimal(18,2) DEFAULT NULL,
  `purchase_price` decimal(18,4) DEFAULT NULL,
  `amount` decimal(18,2) DEFAULT NULL,
  `quality_status` varchar(32) DEFAULT NULL,
  `spdid` varchar(128) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `archived_at` datetime NOT NULL COMMENT '归档时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_order_archived` (`order_id`, `archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购单明细归档表';

-- 5. 销售单(bus_sales)
CREATE TABLE IF NOT EXISTS `archive_bus_sales` (
  `id` int NOT NULL,
  `orderno` varchar(64) DEFAULT NULL,
  `customerid` int DEFAULT NULL,
  `paytype` varchar(255) DEFAULT NULL,
  `salestime` datetime DEFAULT NULL,
  `operateperson` varchar(255) DEFAULT NULL,
  `number` int DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `saleprice` decimal(10,2) DEFAULT NULL,
  `goodsid` int DEFAULT NULL,
  `isdelete` int NOT NULL DEFAULT '0',
  `order_status` int DEFAULT '0' COMMENT '订单状态: 0=正常, 1=已退完',
  `warehouse_id` int DEFAULT NULL COMMENT '仓库ID',
  `location_id` int DEFAULT NULL COMMENT '库位ID',
  `archived_at` datetime NOT NULL COMMENT '归档时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='销售单归档表';

-- 6. 零售单(bus_retail)
CREATE TABLE IF NOT EXISTS `archive_bus_retail` (
  `id` int NOT NULL,
  `orderno` varchar(50) DEFAULT NULL COMMENT '订单号',
  `order_status` int DEFAULT '0' COMMENT '订单状态: 0=正常, 1=已退完',
  `goodsid` int DEFAULT NULL,
  `paytype` varchar(255) DEFAULT NULL,
  `retailtime` datetime DEFAULT NULL,
  `operateperson` varchar(255) DEFAULT NULL,
  `number` int DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `retailprice` decimal(10,2) DEFAULT NULL,
  `isdelete` int NOT NULL DEFAULT '0',
  `warehouse_id` int DEFAULT NULL COMMENT '仓库ID',
  `location_id` int DEFAULT NULL COMMENT '库位ID',
  `archived_at` datetime NOT NULL COMMENT '归档时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='零售单归档表';

-- 7. 入库单(bus_inport)
CREATE TABLE IF NOT EXISTS `archive_bus_inport` (
  `id` int NOT NULL,
  `paytype` varchar(255) DEFAULT NULL,
  `inporttime` datetime DEFAULT NULL,
  `operateperson` varchar(255) DEFAULT NULL,
  `number` int DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `inportprice` double DEFAULT NULL,
  `providerid` int DEFAULT NULL,
  `goodsid` int DEFAULT NULL,
  `orderno` varchar(50) DEFAULT NULL,
  `order_status` int DEFAULT '0',
  `isdelete` int NOT NULL DEFAULT '0',
  `warehouse_id` int DEFAULT NULL COMMENT '仓库ID',
  `location_id` int DEFAULT NULL COMMENT '库位ID',
  `archived_at` datetime NOT NULL COMMENT '归档时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='入库单归档表';

-- 8. 出库单(bus_outport)
CREATE TABLE IF NOT EXISTS `archive_bus_outport` (
  `id` int NOT NULL,
  `providerid` int DEFAULT NULL,
  `paytype` varchar(255) DEFAULT NULL,
  `outputtime` datetime DEFAULT NULL,
  `operateperson` varchar(255) DEFAULT NULL,
  `outportprice` double(10,2) DEFAULT NULL,
  `number` int DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `goodsid` int DEFAULT NULL,
  `inportid` int DEFAULT NULL,
  `isdelete` int NOT NULL DEFAULT '0',
  `archived_at` datetime NOT NULL COMMENT '归档时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='出库单归档表';
