package com.sunlee.bus.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 药企单据号 / 发票号 / SPDID 生成
 */
public final class PharmaNos {

    public static final String STATUS_DRAFT = "草稿";
    public static final String STATUS_CONFIRMED = "已确认";
    public static final String QUALITY_OK = "合格";

    public static final String RECEIVE_PENDING = "待收货";
    public static final String RECEIVE_SIGNED = "已签收";
    public static final String RECEIVE_PARTIAL = "部分签收";
    public static final String RECEIVE_REJECTED = "拒收";
    public static final String RECEIVE_ACTION_SIGN = "签收";
    public static final String RECEIVE_ACTION_REJECT = "拒收";

    public static final String PAID_NONE = "未回款";
    public static final String PAID_PARTIAL = "部分回款";
    public static final String PAID_DONE = "已回款";

    public static final String PAY_CASH = "现结";
    public static final String PAY_MONTH = "月结";

    public static final String ORDER_NORMAL = "正常";
    public static final String ORDER_REVERSAL = "红冲";

    public static final String DRUG_CATEGORY = "药品";

    /** 发票代码后 4 位开票点 */
    public static final String INVOICE_SITE = "0001";
    public static final int INVOICE_LEN = 20;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private PharmaNos() {
    }

    public static String orderNo(String prefix) {
        int seq = ThreadLocalRandom.current().nextInt(1000, 10000);
        return prefix + LocalDateTime.now().format(DAY) + seq;
    }

    public static String spdid() {
        int seq = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "SPD" + LocalDateTime.now().format(TS) + seq;
    }

    /** 12 位代码位：业务日期 YYYYMMDD + 开票点 0001 */
    public static String invoiceCode(LocalDate bizDate) {
        return bizDate.format(DAY) + INVOICE_SITE;
    }

    /** 20 位发票号：12 位代码 + 8 位数据流水 */
    public static String invoiceNo(LocalDate bizDate, long seq) {
        if (seq < 1 || seq > 99_999_999L) {
            throw new IllegalArgumentException("发票数据位超出 8 位");
        }
        return invoiceCode(bizDate) + String.format("%08d", seq);
    }

    public static void assertInvoiceNo(String invoiceNo, LocalDate bizDate) {
        if (invoiceNo == null || !invoiceNo.matches("\\d{20}")) {
            throw new IllegalArgumentException("发票号必须是 20 位数字：前 12 位代码位，后 8 位数据位");
        }
        String day = bizDate.format(DAY);
        if (!invoiceNo.startsWith(day)) {
            throw new IllegalArgumentException("发票号代码位开头必须对应业务日期 " + day);
        }
    }
}
