package com.sunlee.bus.vo;

import com.sunlee.bus.entity.PurchaseOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PurchaseOrderVo extends PurchaseOrder {

    private Integer page = 1;
    private Integer limit = 10;

    /** 业务日起 */
    private String startDate;

    /** 业务日止 */
    private String endDate;

    /** 已入库 / 未入库 */
    private String inboundStatus;
}
