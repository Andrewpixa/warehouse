package com.sunlee.bus.vo;

import com.sunlee.bus.entity.BatchStock;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class BatchStockVo extends BatchStock {

    private Integer page = 1;
    private Integer limit = 10;
    /** all / near / expired */
    private String expireFilter;
    private Integer nearExpireDays = 90;
}
