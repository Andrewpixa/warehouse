package com.sunlee.bus.vo;

import lombok.Data;

@Data
public class VoucherIssue {

    public static final String MISSING_TICKET = "missing_ticket";
    public static final String MISSING_SIGN = "missing_sign";
    public static final String AMOUNT_MISMATCH = "amount_mismatch";

    private String bizType;
    private Long bizId;
    private String orderNo;
    private String invoiceNo;
    private String partnerName;
    private String issueType;
    private String message;
    private String href;

    public static VoucherIssue of(String bizType, Long bizId, String orderNo, String invoiceNo,
                                  String partnerName, String issueType, String message, String href) {
        VoucherIssue issue = new VoucherIssue();
        issue.setBizType(bizType);
        issue.setBizId(bizId);
        issue.setOrderNo(orderNo);
        issue.setInvoiceNo(invoiceNo);
        issue.setPartnerName(partnerName);
        issue.setIssueType(issueType);
        issue.setMessage(message);
        issue.setHref(href);
        return issue;
    }
}
