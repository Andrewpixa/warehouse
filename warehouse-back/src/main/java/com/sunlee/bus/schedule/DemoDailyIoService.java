package com.sunlee.bus.schedule;

import com.sunlee.bus.common.PharmaNos;
import com.sunlee.bus.common.SimInvoiceDocs;
import com.sunlee.bus.service.IBatchStockService;
import com.sunlee.bus.service.IBizVoucherService;
import com.sunlee.bus.service.IPurchaseOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 演示用固定进出库：一张发票多明细；按业务日幂等。
 */
@Slf4j
@Service
public class DemoDailyIoService {

    private static final ZoneId SH = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private IBatchStockService batchStockService;
    @Autowired
    private IBizVoucherService voucherService;
    @Autowired
    private IPurchaseOrderService purchaseOrderService;

    public void runToday() {
        runFor(LocalDate.now(SH));
    }

    @Transactional
    public void runFor(LocalDate bizDate) {
        if (!tableExists("purchase_orders") || !tableExists("sales_orders") || !tableExists("drugs")) {
            return;
        }
        LocalDate day = bizDate == null ? LocalDate.now(SH) : bizDate;
        String tag = day.format(DAY);
        String inNo = "CG-DAILY-" + tag;
        String outNo = "CK-DAILY-" + tag;
        String batch = "DLY" + tag;
        LocalDate prod = day.minusMonths(1);
        LocalDate exp = day.plusYears(2);

        Line[] inbound = {
                line(300012L, batch + "A", "50", "8.90"),
                line(300013L, batch + "B", "30", "3.60"),
                line(300009L, batch + "C", "40", "6.20"),
                line(300001L, batch + "D", "20", "8.00")
        };
        Line[] outbound = {
                line(300012L, batch + "A", "10", "13.50"),
                line(300013L, batch + "B", "8", "5.80"),
                line(300009L, batch + "C", "12", "16.20"),
                line(300001L, batch + "D", "5", "12.80")
        };
        for (Line l : inbound) {
            if (!drugExists(l.drugId)) {
                log.warn("demo daily-io skipped: missing drug {}", l.drugId);
                return;
            }
        }

        if (orderMissing("purchase_orders", inNo)) {
            String invoice = SimInvoiceDocs.supplierInvoiceNo(200010L, day, 88);
            BigDecimal total = sumAmount(inbound);
            jdbc.update("""
                    INSERT INTO purchase_orders (order_no, invoice_no, supplier_id, warehouse_id, salesman_id, checker_id, keeper_id,
                      biz_date, check_result, status, total_amount, created_by, confirmed_by, confirmed_at, remark, created_at, updated_at)
                    VALUES (?, ?, 200010, 1, 2, 5, 3, ?, '合格', '已确认', ?, 1, 1, NOW(), ?, NOW(), NOW())
                    """, inNo, invoice, day, total, "演示定时入库·一张发票四明细");
            Long oid = idByNo("purchase_orders", inNo);
            for (Line l : inbound) {
                insertPurchaseItem(oid, l, prod, exp);
                batchStockService.increase(l.drugId, 1L, l.batchNo, PharmaNos.QUALITY_OK, l.qty, prod, exp);
            }
            purchaseOrderService.ensureSimulatedDocs(oid, true);
            log.info("demo daily inbound {}", inNo);
        }

        if (orderMissing("sales_orders", outNo)) {
            BigDecimal total = sumAmount(outbound);
            String invoice = uniqueSalesInvoice(day);
            jdbc.update("""
                    INSERT INTO sales_orders (order_no, invoice_no, order_type, customer_id, warehouse_id, salesman_id, reviewer_id,
                      biz_date, pay_type, status, total_amount, paid_status, paid_amount, created_by, confirmed_by, confirmed_at,
                      ship_time, einvoice_no, receive_status, remark, created_at, updated_at, order_channel, preprocess_status)
                    VALUES (?, ?, '正常', 100001, 1, 4, 5, ?, '月结', '已确认', ?, '未回款', 0, 1, 1, NOW(), NOW(), ?, '待收货',
                      ?, NOW(), NOW(), 'OFFLINE', '已通过')
                    """, outNo, invoice, day, total, invoice, "演示定时出库·一张发票四明细");
            Long oid = idByNo("sales_orders", outNo);
            StringBuilder rows = new StringBuilder();
            for (Line l : outbound) {
                insertSalesItem(oid, l, exp);
                batchStockService.decrease(l.drugId, 1L, l.batchNo, PharmaNos.QUALITY_OK, l.qty);
                rows.append(htmlRow(drugName(l.drugId), l.batchNo, l.qty, l.price));
            }
            String html = "<h1>电子发票（模拟·我方开具）</h1>"
                    + "<p>销货方：药衡医药</p><p>购货方：南方医科大学南方医院</p>"
                    + "<p>全电发票号码：" + SimInvoiceDocs.escape(invoice) + "</p>"
                    + "<p>开票日期：" + day + "</p>"
                    + "<table><tr><th>品种</th><th>批号</th><th>数量</th><th>单价</th></tr>"
                    + rows + "</table><p>价税合计：" + total + "</p>";
            String path = SimInvoiceDocs.writeHtml("einvoice.html", "电子发票", html);
            jdbc.update("UPDATE sales_orders SET einvoice_path = ? WHERE id = ?", path, oid);
            voucherService.replaceGeneratedFile(PharmaNos.BIZ_SALES, oid, "invoice",
                    "电子发票-" + invoice + ".html", html);
            log.info("demo daily outbound {}", outNo);
        }
    }

    private record Line(Long drugId, String batchNo, BigDecimal qty, BigDecimal price) {
        BigDecimal amount() {
            return qty.multiply(price);
        }
    }

    private static Line line(Long drugId, String batch, String qty, String price) {
        return new Line(drugId, batch, new BigDecimal(qty), new BigDecimal(price));
    }

    private static BigDecimal sumAmount(Line[] lines) {
        BigDecimal t = BigDecimal.ZERO;
        for (Line l : lines) {
            t = t.add(l.amount());
        }
        return t;
    }

    private void insertPurchaseItem(Long orderId, Line l, LocalDate prod, LocalDate exp) {
        jdbc.update("""
                INSERT INTO purchase_order_items (order_id, drug_id, batch_no, production_date, expire_date,
                  receive_qty, qualified_qty, stock_in_qty, purchase_price, amount, quality_status, spdid, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '合格', ?, NOW(), NOW())
                """, orderId, l.drugId, l.batchNo, prod, exp, l.qty, l.qty, l.qty, l.price, l.amount(), PharmaNos.spdid());
    }

    private void insertSalesItem(Long orderId, Line l, LocalDate exp) {
        jdbc.update("""
                INSERT INTO sales_order_items (order_id, drug_id, batch_no, expire_date, qty, sale_price, amount,
                  spdid, quality_status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, '合格', NOW(), NOW())
                """, orderId, l.drugId, l.batchNo, exp, l.qty, l.price, l.amount(), PharmaNos.spdid());
    }

    private String uniqueSalesInvoice(LocalDate day) {
        long seq = 800_000_000L + Long.parseLong(day.format(DAY)) % 100_000_000L;
        String no = PharmaNos.invoiceNo(day, seq);
        while (Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT COUNT(*) > 0 FROM sales_orders WHERE invoice_no = ?", Boolean.class, no))) {
            seq++;
            no = PharmaNos.invoiceNo(day, seq);
        }
        return no;
    }

    private String htmlRow(String name, String batch, BigDecimal qty, BigDecimal price) {
        return "<tr><td>" + SimInvoiceDocs.escape(name) + "</td><td>" + SimInvoiceDocs.escape(batch)
                + "</td><td>" + qty.stripTrailingZeros().toPlainString() + "</td><td>" + price + "</td></tr>";
    }

    private String drugName(Long id) {
        List<String> names = jdbc.query(
                "SELECT COALESCE(generic_name, trade_name, code) FROM drugs WHERE id = ?",
                (rs, i) -> rs.getString(1), id);
        return names.isEmpty() ? String.valueOf(id) : names.get(0);
    }

    private boolean drugExists(Long id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM drugs WHERE id = ?", Integer.class, id);
        return n != null && n > 0;
    }

    private boolean orderMissing(String table, String orderNo) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE order_no = ?", Integer.class, orderNo);
        return n == null || n == 0;
    }

    private Long idByNo(String table, String orderNo) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE order_no = ?", Long.class, orderNo);
    }

    private boolean tableExists(String table) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class, table);
        return n != null && n > 0;
    }
}
