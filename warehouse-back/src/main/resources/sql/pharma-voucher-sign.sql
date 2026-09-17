-- 购进/销售合法票据关联 + 电子签字留痕
SET NAMES utf8mb4;

SET @col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_orders' AND COLUMN_NAME = 'invoice_no'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE purchase_orders
     ADD COLUMN invoice_no VARCHAR(64) DEFAULT NULL COMMENT ''供应商发票号'' AFTER order_no',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS biz_attachments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  biz_type VARCHAR(16) NOT NULL COMMENT 'purchase/sales',
  biz_id BIGINT NOT NULL,
  attach_type VARCHAR(16) NOT NULL COMMENT 'invoice/packing',
  file_path VARCHAR(255) NOT NULL,
  file_name VARCHAR(128) DEFAULT NULL,
  created_by BIGINT DEFAULT NULL,
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_biz (biz_type, biz_id, attach_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单据票据附件（发票/随货同行单）';

CREATE TABLE IF NOT EXISTS biz_signatures (
  id BIGINT NOT NULL AUTO_INCREMENT,
  biz_type VARCHAR(16) NOT NULL COMMENT 'purchase/sales',
  biz_id BIGINT NOT NULL,
  sign_role VARCHAR(32) NOT NULL COMMENT 'purchase_check/purchase_keep/delivery/customer',
  signer_name VARCHAR(64) NOT NULL,
  sign_source VARCHAR(16) NOT NULL COMMENT 'pad/photo',
  image_path VARCHAR(255) NOT NULL,
  signed_at DATETIME NOT NULL,
  created_by BIGINT DEFAULT NULL,
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_role (biz_type, biz_id, sign_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单据电子签字';
