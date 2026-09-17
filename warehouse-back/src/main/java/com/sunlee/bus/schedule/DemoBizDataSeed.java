package com.sunlee.bus.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 启动时补演示数据：信誉额、分货、缺货、到货异常、销退、冲账、物流、统筹、开票草稿、厂家账号。
 * 按单号去重，可重复启动。
 */
@Slf4j
@Order(40)
@Component
public class DemoBizDataSeed implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        try {
            if (!tableExists("ops_flow_docs") || !tableExists("customers")) {
                return;
            }
            try {
                seedExtraMasters();
            } catch (Exception e) {
                log.warn("extra masters skipped: {}", e.getMessage());
            }
            seedCreditForEveryCustomer();
            seedOtherOps();
            seedMakerAccounts();
            seedInvoiceDrafts();
            log.info("demo biz data seeded");
        } catch (Exception e) {
            log.warn("demo biz data skipped: {}", e.getMessage());
        }
    }

    private void seedExtraMasters() {
        jdbc.update("""
                INSERT INTO customers (id, code, name, customer_type, license_no, contact, phone, address, settle_type, status, remark, created_at, updated_at)
                VALUES
                  (100016, '100016', '广州医科大学附属第一医院', '医院', '粤DA1010001', '药剂科-冯主任', '020-83062114', '广州市越秀区沿江路151号', '月结', 1, '演示-证照齐全', NOW(), NOW()),
                  (100017, '100017', '暨南大学附属第一医院', '医院', '粤DA1011001', '药学部-梁主任', '020-38688888', '广州市天河区黄埔大道西613号', '月结', 1, NULL, NOW(), NOW()),
                  (100018, '100018', '广州中医药大学第一附属医院', '医院', '粤DA1012001', '药剂科-邓主任', '020-36591912', '广州市白云区机场路16号', '月结', 1, NULL, NOW(), NOW()),
                  (100019, '100019', '海珠区第二人民医院', '医院', '粤DA1013001', '药库-蔡主管', '020-84418800', '广州市海珠区工业大道北', '月结', 1, NULL, NOW(), NOW()),
                  (100020, '100020', '老百姓大药房（体育西店）', '药店', '粤DA1014001', '王店长', '020-38780011', '广州市天河区体育西路109号', '现结', 1, NULL, NOW(), NOW()),
                  (100021, '100021', '健民医药（番禺店）', '药店', '粤DA1015001', '刘店长', '020-34681122', '广州市番禺区市桥街大北路', '现结', 1, NULL, NOW(), NOW()),
                  (100022, '100022', '黄埔区红山街社区卫生服务中心', '诊所', '粤DA1016001', '陈医生', '020-82110088', '广州市黄埔区红山路', '月结', 1, NULL, NOW(), NOW()),
                  (100023, '100023', '证照过期演示诊所', '诊所', NULL, '临时联系人', '020-00000000', '广州市演示地址', '现结', 0, '停用+无证照，预处理应失败', NOW(), NOW())
                ON DUPLICATE KEY UPDATE name = VALUES(name), license_no = VALUES(license_no), status = VALUES(status)
                """);
        jdbc.update("""
                INSERT INTO suppliers (id, code, name, license_no, contact, phone, address, settle_type, first_camp_ok, status, remark, created_at, updated_at)
                VALUES
                  (200014, '200014', '哈药集团制药总厂', '黑AA1008001', '周经理', '0451-80880005', '哈尔滨市道里区工厂街', '月结', 1, 1, NULL, NOW(), NOW()),
                  (200015, '200015', '华北制药股份有限公司', '冀AA1009001', '吴经理', '0311-80880006', '石家庄市和平东路388号', '月结', 0, 1, '待首营', NOW(), NOW())
                ON DUPLICATE KEY UPDATE name = VALUES(name)
                """);
    }

    private void seedCreditForEveryCustomer() {
        List<Map<String, Object>> customers = jdbc.queryForList(
                "SELECT id, name, settle_type, status FROM customers ORDER BY id");
        int i = 0;
        for (Map<String, Object> c : customers) {
            Long id = ((Number) c.get("id")).longValue();
            String name = String.valueOf(c.get("name"));
            String settle = c.get("settle_type") == null ? "月结" : String.valueOf(c.get("settle_type"));
            int status = c.get("status") == null ? 1 : ((Number) c.get("status")).intValue();
            String no = "XY-C" + id;
            boolean draft = status == 0 || i % 7 == 6;
            BigDecimal amt = status == 0 ? BigDecimal.ZERO
                    : ("现结".equals(settle) ? new BigDecimal("30000") : new BigDecimal(80000 + (i % 8) * 25000));
            if (id == 100001L) {
                amt = new BigDecimal("200000");
            }
            if (id == 100011L) {
                amt = new BigDecimal("5000");
            }
            upsertOps(no, "CREDIT", "2026-09-01", draft ? "草稿" : "已生效",
                    id, null, null, null, null, amt, BigDecimal.ZERO,
                    name + "信誉额" + amt.toPlainString() + "元",
                    draft ? "待审核授信" : settle + "授信",
                    draft ? "维护信誉额后点生效，开票预处理才认" : "开票预处理校验");
            if (i % 5 == 0 && status == 1) {
                upsertOps("XY-ADJ" + id, "CREDIT", "2026-09-16", "草稿",
                        id, null, null, null, null, amt.add(new BigDecimal("20000")), BigDecimal.ZERO,
                        name + "申请调增信誉额", "额度不够", "演示：草稿调额，未生效不影响当前授信");
            }
            i++;
        }
    }

    private void seedOtherOps() {
        upsertOps("QH-DEMO01", "STOCKOUT", "2026-09-16", "已登记", 100001L, null, 300009L, 1L, null,
                BigDecimal.ZERO, new BigDecimal("30"), "南方医院缺奥美拉唑", "现货不足", "采购按下限补货");
        upsertOps("QH-DEMO02", "STOCKOUT", "2026-09-16", "已登记", 100012L, null, 300015L, 1L, null,
                BigDecimal.ZERO, new BigDecimal("50"), "海王星辰缺必理通", "低于下限", null);
        upsertOps("QH-DEMO03", "STOCKOUT", "2026-09-16", "已补货", 100016L, null, 300001L, 1L, "CG20260915001",
                BigDecimal.ZERO, new BigDecimal("80"), "广医一院阿莫仙已补货", "已到货", null);
        upsertOps("QH-DEMO04", "STOCKOUT", "2026-09-17", "草稿", 100020L, null, 300011L, 1L, null,
                BigDecimal.ZERO, new BigDecimal("40"), "老百姓药房缺芬必得", "开票缺货", "待登记");
        upsertOps("QH-DEMO05", "STOCKOUT", "2026-09-17", "已登记", 100018L, null, 300002L, 3L, null,
                BigDecimal.ZERO, new BigDecimal("12"), "中医一院缺胰岛素", "冷链缺货", null);

        upsertOps("YC-DEMO01", "INBOUND_EX", "2026-09-14", "待复检", null, 200010L, 300012L, 2L, "HT20260901",
                BigDecimal.ZERO, new BigDecimal("100"), "扬子江左氧缺检验报告", "缺少资料", "入待复检");
        upsertOps("YC-DEMO02", "INBOUND_EX", "2026-09-14", "待处理", null, 200011L, 300013L, 2L, "HT20260902",
                BigDecimal.ZERO, new BigDecimal("40"), "齐鲁头孢外箱压损", "货品残损", null);
        upsertOps("YC-DEMO03", "INBOUND_EX", "2026-09-13", "合格", null, 200001L, 300014L, 3L, "HT20260903",
                BigDecimal.ZERO, new BigDecimal("20"), "冷链超温有保证函", "超温", "质量已放行");
        upsertOps("YC-DEMO04", "INBOUND_EX", "2026-09-16", "拒收", null, 200015L, 300006L, 2L, "HT20260916",
                BigDecimal.ZERO, new BigDecimal("60"), "华北制药盐水与合同不符", "来货与合同不符", "拒收离场");
        upsertOps("YC-DEMO05", "INBOUND_EX", "2026-09-16", "待处理", null, 200014L, 300005L, 2L, "HT20260917",
                BigDecimal.ZERO, new BigDecimal("30"), "哈药甘草片近效期争议", "效期", "待质量");

        upsertOps("XT-DEMO01", "RETURN_NOTICE", "2026-09-15", "已释放", 100002L, null, 300003L, 4L, "CK20260907002",
                new BigDecimal("227.50"), new BigDecimal("35"), "大参林拒收VC", "客户拒收", "下次送点收回");
        upsertOps("XT-DEMO02", "RETURN_NOTICE", "2026-09-12", "已入库", 100001L, null, 300001L, 4L, "CK20260907001",
                new BigDecimal("128.00"), new BigDecimal("10"), "南方医院滞销退阿莫仙", "滞销", null);
        upsertOps("XT-DEMO03", "RETURN_NOTICE", "2026-09-16", "草稿", 100013L, null, 300016L, 4L, "CK20260914001",
                new BigDecimal("279.00"), new BigDecimal("10"), "红会医院开错票拟退", "开错票", "待业务员签名");
        upsertOps("XT-DEMO04", "RETURN_NOTICE", "2026-09-16", "已签名", 100017L, null, 300008L, 4L, null,
                new BigDecimal("428.00"), new BigDecimal("10"), "暨大附一召回立普妥", "厂家召回", "待运营释放");

        upsertOps("CZ-DEMO01", "OFFSET", "2026-09-16", "草稿", 100001L, null, null, null, "20260907000100000001",
                new BigDecimal("512.00"), BigDecimal.ZERO, "南方医院公对公冲发票", "公对公", "金额与到账一致");
        upsertOps("CZ-DEMO02", "OFFSET", "2026-09-16", "草稿", 100003L, null, null, null, "20260907000100000003",
                new BigDecimal("84.00"), BigDecimal.ZERO, "阳光社区冲账差50", "到账不一致", "需挂账");
        upsertOps("CZ-DEMO03", "OFFSET", "2026-09-17", "已冲账", 100013L, null, null, null, "20260914000100000012",
                new BigDecimal("100.00"), BigDecimal.ZERO, "红会医院部分回款已冲", "支票", null);
        upsertOps("CZ-DEMO04", "OFFSET", "2026-09-17", "草稿", 100010L, null, null, null, "20260915000100000010",
                new BigDecimal("856.00"), BigDecimal.ZERO, "中山一全额冲账待确认", "公对公", null);

        upsertOps("FH-DEMO01", "ALLOCATE", "2026-09-15", "待分货", 100011L, null, 300012L, 1L, "WD2026091508",
                BigDecimal.ZERO, new BigDecimal("80"), "省医网单左氧待分货", "需要分货", "预处理应卡住");
        upsertOps("FH-DEMO02", "ALLOCATE", "2026-09-14", "已分货", 100010L, null, 300008L, 1L, "WD2026091402",
                BigDecimal.ZERO, new BigDecimal("20"), "中山一立普妥已分货", "分货完成", "可开票");
        upsertOps("FH-DEMO03", "ALLOCATE", "2026-09-16", "待分货", 100016L, null, 300001L, 1L, "WD2026091601",
                BigDecimal.ZERO, new BigDecimal("40"), "广医一院阿莫仙抢货", "多客户配额", null);
        upsertOps("FH-DEMO04", "ALLOCATE", "2026-09-16", "已分货", 100018L, null, 300002L, 3L, "WD2026091602",
                BigDecimal.ZERO, new BigDecimal("6"), "中医一院胰岛素已分货", "冷链配额", null);
        upsertOps("FH-DEMO05", "ALLOCATE", "2026-09-17", "待分货", 100020L, null, 300010L, 1L, null,
                BigDecimal.ZERO, new BigDecimal("50"), "老百姓药房思密达待分", "药店配额", null);

        upsertOps("WL-DEMO01", "LOGISTICS", "2026-09-15", "草稿", 100014L, null, null, 1L, "CKPLAN001",
                BigDecimal.ZERO, new BigDecimal("120"), "番禺中心超100件预约", "超100件", "需预约入库");
        upsertOps("WL-DEMO02", "LOGISTICS", "2026-09-15", "已同意", 100013L, null, null, 1L, null,
                BigDecimal.ZERO, BigDecimal.ZERO, "红会医院午后送达", "特殊送货", "运营已同意");
        upsertOps("WL-DEMO03", "LOGISTICS", "2026-09-16", "草稿", 100017L, null, null, 1L, null,
                BigDecimal.ZERO, BigDecimal.ZERO, "暨大附一取消订单通知仓财", "取消订单", null);
        upsertOps("WL-DEMO04", "LOGISTICS", "2026-09-17", "已同意", 100019L, null, null, 1L, "CKPLAN002",
                BigDecimal.ZERO, new BigDecimal("160"), "海珠二院整车预约", "超100件", null);

        upsertOps("TC-DEMO01", "QUOTA", "2026-09-15", "草稿", null, null, null, 1L, null,
                BigDecimal.ZERO, new BigDecimal("800"), "广深惠常温库加统筹", "超统筹值", "采购转物流");
        upsertOps("TC-DEMO02", "QUOTA", "2026-09-10", "已生效", null, null, null, 3L, null,
                BigDecimal.ZERO, new BigDecimal("120"), "冷链仓统筹已增加", "库容", null);
        upsertOps("TC-DEMO03", "QUOTA", "2026-09-16", "草稿", null, null, null, 2L, null,
                BigDecimal.ZERO, new BigDecimal("200"), "白云待验区申请加库容", "待验爆仓", null);
        upsertOps("TC-DEMO04", "QUOTA", "2026-09-17", "已生效", null, null, null, 1L, null,
                BigDecimal.ZERO, new BigDecimal("500"), "广深惠二期统筹", "库容", null);
    }

    private void seedMakerAccounts() {
        if (!tableExists("maker_flow_accounts")) {
            return;
        }
        jdbc.update("""
                INSERT INTO maker_flow_accounts (login_name, password, supplier_id, status, effective_at, remark, created_at)
                SELECT 'gzyy', '123456', 200001, 1, '2026-09-15 00:00:00', '广州医药厂家流向', NOW()
                FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM maker_flow_accounts WHERE login_name = 'gzyy')
                """);
        jdbc.update("""
                INSERT INTO maker_flow_accounts (login_name, password, supplier_id, status, effective_at, remark, created_at)
                SELECT 'yzj', '123456', 200010, 1, '2026-09-16 00:00:00', '扬子江', NOW()
                FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM maker_flow_accounts WHERE login_name = 'yzj')
                """);
        jdbc.update("""
                INSERT INTO maker_flow_accounts (login_name, password, supplier_id, status, effective_at, remark, created_at)
                SELECT 'qlzy', '123456', 200011, 1, '2026-09-16 00:00:00', '齐鲁制药', NOW()
                FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM maker_flow_accounts WHERE login_name = 'qlzy')
                """);
        jdbc.update("""
                INSERT INTO maker_flow_accounts (login_name, password, supplier_id, status, effective_at, remark, created_at)
                SELECT 'pfizer', '123456', 200001, 1, '2026-09-17 00:00:00', '演示辉瑞流向查询', NOW()
                FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM maker_flow_accounts WHERE login_name = 'pfizer')
                """);
    }

    private void seedInvoiceDrafts() {
        if (!tableExists("sales_orders") || !tableExists("batch_stocks")) {
            return;
        }
        List<Map<String, Object>> stocks = jdbc.queryForList("""
                SELECT drug_id, warehouse_id, batch_no, expire_date, qty
                FROM batch_stocks WHERE qty > 0 AND quality_status = '合格' ORDER BY id LIMIT 8
                """);
        if (stocks.isEmpty()) {
            return;
        }
        insertDraft("CK-BMS-001", "OFFLINE", null, 100001L, "线下口头计划·南方医院",
                stocks.get(0), new BigDecimal("12.80"), "待预处理");
        insertDraft("CK-NET-001", "PLATFORM", "WD20260917001", 100010L, "全药网网单·中山一",
                stocks.get(Math.min(1, stocks.size() - 1)), new BigDecimal("42.80"), "待预处理");
        insertDraft("CK-NET-002", "PLATFORM", "WD20260917002", 100011L, "药交平台网单·省医（待分货应卡住）",
                stocks.get(Math.min(2, stocks.size() - 1)), new BigDecimal("13.50"), "待预处理");
        insertDraft("CK-BMS-002", "OFFLINE", null, 100002L, "表格计划·大参林",
                stocks.get(Math.min(3, stocks.size() - 1)), new BigDecimal("6.50"), "待预处理");
        insertDraft("CK-BMS-003", "OFFLINE", null, 100020L, "开票员录入·老百姓（待分货）",
                stocks.get(Math.min(4, stocks.size() - 1)), new BigDecimal("19.60"), "待预处理");
        insertDraft("CK-BMS-004", "OFFLINE", null, 100023L, "无证照停用客户·预处理应失败",
                stocks.get(0), BigDecimal.ZERO, "待预处理");
        insertDraft("CK-BMS-005", "OFFLINE", null, 100016L, "广医一院·待分货",
                stocks.get(Math.min(5, stocks.size() - 1)), new BigDecimal("12.80"), "待预处理");
        insertDraft("CK-NET-003", "PLATFORM", "WD20260917003", 100018L, "网单·中医一院已分货可过闸",
                stocks.get(Math.min(6, stocks.size() - 1)), new BigDecimal("68.00"), "待预处理");
    }

    private void insertDraft(String orderNo, String channel, String platformNo, Long customerId, String remark,
                             Map<String, Object> stock, BigDecimal price, String prep) {
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM sales_orders WHERE order_no = ?", Integer.class, orderNo);
        if (exists != null && exists > 0) {
            return;
        }
        Long drugId = ((Number) stock.get("drug_id")).longValue();
        Long whId = ((Number) stock.get("warehouse_id")).longValue();
        String batch = String.valueOf(stock.get("batch_no"));
        Object expire = stock.get("expire_date");
        jdbc.update("""
                INSERT INTO sales_orders (order_no, invoice_no, order_type, customer_id, warehouse_id, salesman_id, biz_date, pay_type,
                  status, total_amount, paid_status, paid_amount, created_by, remark, created_at, updated_at,
                  order_channel, platform_no, preprocess_status)
                VALUES (?, NULL, '正常', ?, ?, 4, '2026-09-17', '月结', '草稿', ?, '未回款', 0, 1, ?, NOW(), NOW(), ?, ?, ?)
                """, orderNo, customerId, whId, price, remark, channel, platformNo, prep);
        Long oid = jdbc.queryForObject("SELECT id FROM sales_orders WHERE order_no = ?", Long.class, orderNo);
        jdbc.update("""
                INSERT INTO sales_order_items (order_id, drug_id, batch_no, expire_date, qty, sale_price, amount, spdid, quality_status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1.000, ?, ?, ?, '合格', NOW(), NOW())
                """, oid, drugId, batch, expire, price, price, "SPD" + orderNo.replace("-", ""));
    }

    private void upsertOps(String no, String type, String date, String status, Long customerId, Long supplierId,
                           Long drugId, Long warehouseId, String related, BigDecimal amount, BigDecimal qty,
                           String title, String reason, String remark) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM ops_flow_docs WHERE doc_no = ?", Integer.class, no);
        if (n != null && n > 0) {
            jdbc.update("""
                    UPDATE ops_flow_docs SET status=?, customer_id=?, supplier_id=?, drug_id=?, warehouse_id=?,
                      related_no=?, amount=?, qty=?, title=?, reason=?, remark=?, updated_at=NOW()
                    WHERE doc_no=?
                    """, status, customerId, supplierId, drugId, warehouseId, related, amount, qty, title, reason, remark, no);
            return;
        }
        jdbc.update("""
                INSERT INTO ops_flow_docs (doc_type, doc_no, biz_date, status, customer_id, supplier_id, drug_id, warehouse_id,
                  related_no, amount, qty, title, reason, remark, created_by, created_at, updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,1,NOW(),NOW())
                """, type, no, date, status, customerId, supplierId, drugId, warehouseId, related, amount, qty, title, reason, remark);
    }

    private boolean tableExists(String table) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class, table);
        return n != null && n > 0;
    }
}
