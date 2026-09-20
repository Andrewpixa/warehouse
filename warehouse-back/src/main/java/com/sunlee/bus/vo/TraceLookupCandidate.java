package com.sunlee.bus.vo;

import lombok.Data;

@Data
public class TraceLookupCandidate {

    /** 入库 / 出库 */
    private String billType;

    private Long orderId;

    private String orderNo;

    private String invoiceNo;

    private String spdid;

    private String batchNo;

    private String drugName;

    /** 供应商或客户 */
    private String partyName;
}
