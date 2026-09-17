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

    public static final String BIZ_PURCHASE = "purchase";
    public static final String BIZ_SALES = "sales";

    /**
     * 全电发票 20 位，对齐公司票样 26957000000121085238：
     * 第 1-2 位开票年度，第 3-10 位开票方赋码段，第 11-20 位顺序号。
     */
    public static final String EINVOICE_ISSUER = "95700000";
    public static final int INVOICE_LEN = 20;
    public static final int INVOICE_SEQ_START = 121_085_238;

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

    /** 前 10 位：年度 2 位 + 赋码段 8 位 */
    public static String invoiceCode(LocalDate bizDate) {
        return year2(bizDate) + EINVOICE_ISSUER;
    }

    public static String invoiceNo(LocalDate bizDate, long seq) {
        if (seq < 1 || seq > 9_999_999_999L) {
            throw new IllegalArgumentException("全电发票顺序号须为 1～10 位数字");
        }
        return invoiceCode(bizDate) + String.format("%010d", seq);
    }

    public static void assertInvoiceNo(String invoiceNo, LocalDate bizDate) {
        if (invoiceNo == null || !invoiceNo.matches("\\d{20}")) {
            throw new IllegalArgumentException("须为 20 位全电发票号码（年度2位+赋码段8位+顺序号10位）");
        }
        if (bizDate != null && !invoiceNo.startsWith(invoiceCode(bizDate))) {
            throw new IllegalArgumentException("全电号码须为年度 " + year2(bizDate) + " + 赋码段 " + EINVOICE_ISSUER + " + 10 位顺序号");
        }
    }

    public static long parseSeq(String invoiceNo) {
        if (invoiceNo != null && invoiceNo.length() == INVOICE_LEN) {
            return Long.parseLong(invoiceNo.substring(10));
        }
        return 0L;
    }

    private static String year2(LocalDate bizDate) {
        return String.format("%02d", bizDate.getYear() % 100);
    }
}
