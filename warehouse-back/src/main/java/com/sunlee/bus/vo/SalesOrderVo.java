package com.sunlee.bus.vo;

import com.sunlee.bus.entity.SalesOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class SalesOrderVo extends SalesOrder {

    private Integer page = 1;
    private Integer limit = 10;

    /** 签收 / 拒收 */
    private String receiveAction;
}
