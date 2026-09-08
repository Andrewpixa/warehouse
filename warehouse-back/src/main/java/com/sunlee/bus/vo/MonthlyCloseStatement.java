package com.sunlee.bus.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class MonthlyCloseStatement {

    private String yearMonth;
    private boolean closed;
    private Date closedAt;
    private String closedByName;
    private Summary summary = new Summary();
    private List<PartnerRow> customers = new ArrayList<>();
    private List<PartnerRow> suppliers = new ArrayList<>();

    @Data
    public static class Summary {
        private int customerCount;
        private BigDecimal salesAmount = BigDecimal.ZERO;
        private BigDecimal paidAmount = BigDecimal.ZERO;
        private BigDecimal unpaidAmount = BigDecimal.ZERO;
        private int supplierCount;
        private BigDecimal purchaseAmount = BigDecimal.ZERO;
    }

    @Data
    public static class PartnerRow {
        private Long partnerId;
        private String partnerName;
        private String payType;
        private int orderCount;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private BigDecimal paidAmount = BigDecimal.ZERO;
        private BigDecimal unpaidAmount = BigDecimal.ZERO;
    }
}
