package com.sunlee.bus.vo;

import com.sunlee.bus.entity.PurchaseOrder;
import com.sunlee.bus.entity.SalesOrder;
import com.sunlee.bus.entity.TraceCode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TraceLookupResult {

    private String keyword;

    /**
     * TRACE_CODE / SPDID / PURCHASE_ORDER / PURCHASE_INVOICE / BATCH / SALES_INVOICE / SALES_ORDER
     */
    private String matchType;

    private String matchTypeLabel;

    /** 入库 / 出库 */
    private String billType;

    private PurchaseOrder purchase;

    private SalesOrder outbound;

    private List<TraceCode> traces = new ArrayList<>();

    /** 批号等多候选时返回，前端点选后再查单号 */
    private List<TraceLookupCandidate> candidates = new ArrayList<>();
}
