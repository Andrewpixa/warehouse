package com.sunlee.bus.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerDebtVo {

    private Long customerId;

    private String customerName;

    private Integer invoiceCount;

    private BigDecimal totalAmount = BigDecimal.ZERO;

    private BigDecimal paidAmount = BigDecimal.ZERO;

    private BigDecimal unpaidAmount = BigDecimal.ZERO;
}
