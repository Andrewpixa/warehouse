package com.sunlee.bus.vo;

import com.sunlee.bus.entity.GoodsStock;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class GoodsStockVo extends GoodsStock {

    private Integer page = 1;
    private Integer limit = 10;
}
