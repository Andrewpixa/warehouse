package com.sunlee.bus.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PurchaseInvoiceLineVo {

    private Long itemId;
    private Long orderId;
    private String orderNo;
    private String invoiceNo;
    private Long supplierId;
    private String supplierName;
    private String warehouseName;
    private LocalDate bizDate;
    private String status;
    private String inboundStatus;
    private String drugName;
    private String drugSpec;
    private String batchNo;
    private BigDecimal receiveQty;
    private BigDecimal stockInQty;
    private BigDecimal purchasePrice;
    private BigDecimal amount;
    private String qualityStatus;
}
