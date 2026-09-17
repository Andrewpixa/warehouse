package com.sunlee.bus.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Order(10)
@Component
public class OpsFlowSchemaInit implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("""
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
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司运作协同单据'
                    """);
            jdbcTemplate.execute("""
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
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协同单据明细'
                    """);
            jdbcTemplate.execute("""
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
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂家流向查询账号'
                    """);
            log.info("ops_flow schema ready");
            addCol("sys_dept", "dept_code", "VARCHAR(32) DEFAULT NULL COMMENT '部门编码'");
            addCol("sys_dept", "dept_type", "VARCHAR(16) DEFAULT NULL COMMENT '部门类型'");
            addCol("sys_dept", "manager_user_id", "INT DEFAULT NULL COMMENT '负责人'");
            addCol("sys_dept", "phone", "VARCHAR(32) DEFAULT NULL");
            addCol("sys_dept", "gsp_roles", "VARCHAR(255) DEFAULT NULL COMMENT 'GSP职责'");
            addCol("sales_orders", "order_channel", "VARCHAR(16) DEFAULT 'OFFLINE' COMMENT 'PLATFORM网单/OFFLINE线下BMS'");
            addCol("sales_orders", "platform_no", "VARCHAR(64) DEFAULT NULL COMMENT '全药网/药交平台单号'");
            addCol("sales_orders", "preprocess_status", "VARCHAR(16) DEFAULT '待预处理'");
            addCol("sales_orders", "credit_ok", "TINYINT DEFAULT 0");
            addCol("sales_orders", "license_ok", "TINYINT DEFAULT 0");
            addCol("sales_orders", "allocate_ok", "TINYINT DEFAULT 0");
            addCol("sales_orders", "price_locked", "TINYINT DEFAULT 0");
            addCol("sales_orders", "preprocess_remark", "VARCHAR(500) DEFAULT NULL");
        } catch (Exception e) {
            log.warn("ops_flow schema init skipped: {}", e.getMessage());
        }
    }

    private void addCol(String table, String col, String def) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, col);
        if (n != null && n == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + col + " " + def);
        }
    }
}
