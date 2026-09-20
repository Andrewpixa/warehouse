-- 银行到账流水（销售应收一期；应付第二期复用规则）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS bank_receipts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  receipt_no VARCHAR(32) NOT NULL,
  customer_id BIGINT NOT NULL,
  received_date DATE NOT NULL,
  amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  channel VARCHAR(16) NOT NULL DEFAULT '公对公' COMMENT '公对公/支票',
  voucher_no VARCHAR(64) DEFAULT NULL,
  payer_name VARCHAR(128) DEFAULT NULL,
  status VARCHAR(16) NOT NULL DEFAULT '未认领' COMMENT '未认领/部分认领/已认领/挂账',
  remark VARCHAR(500) DEFAULT NULL,
  created_by BIGINT DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bank_receipt_no (receipt_no),
  KEY idx_bank_receipt_cust (customer_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行到账流水';
