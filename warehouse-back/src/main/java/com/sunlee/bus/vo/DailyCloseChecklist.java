package com.sunlee.bus.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 日清检查清单：未清项 + 是否已日清。
 */
@Data
public class DailyCloseChecklist {

    private LocalDate bizDate;
    private boolean cleared;
    private boolean closed;
    private Date closedAt;
    private String closedByName;
    private int blockerCount;
    private Summary summary = new Summary();
    private List<OrderRow> draftPurchases = new ArrayList<>();
    private List<OrderRow> draftOutbounds = new ArrayList<>();
    private List<OrderRow> draftSurplus = new ArrayList<>();
    private List<TraceRow> missingTraces = new ArrayList<>();
        private List<OrderRow> cashUnpaid = new ArrayList<>();
    private List<OrderRow> monthlyUnpaid = new ArrayList<>();
    private List<OrderRow> pendingReceipts = new ArrayList<>();

    @Data
    public static class Summary {
        private long draftPurchaseCount;
        private long draftOutboundCount;
        private long draftSurplusCount;
        private long confirmedPurchaseCount;
        private BigDecimal confirmedPurchaseAmount = BigDecimal.ZERO;
        private long confirmedOutboundCount;
        private BigDecimal confirmedOutboundAmount = BigDecimal.ZERO;
        private long missingTraceCount;
        private long cashUnpaidCount;
        private long monthlyUnpaidCount;
        private long pendingReceiptCount;
    }

    @Data
    public static class OrderRow {
        private Long id;
        private String orderNo;
        private String invoiceNo;
        private String partnerName;
        private String warehouseName;
        private String payType;
        private String paidStatus;
        private BigDecimal totalAmount;
        private String remark;
    }

    @Data
    public static class TraceRow {
        private Long orderId;
        private String orderNo;
        private String invoiceNo;
        private String customerName;
        private String drugName;
        private String batchNo;
        private String spdid;
        private BigDecimal qty;
    }
}
