package com.sunlee.bus.vo;

import com.sunlee.bus.entity.PurchaseOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PurchaseOrderVo extends PurchaseOrder {

    private Integer page = 1;
    private Integer limit = 10;
}
