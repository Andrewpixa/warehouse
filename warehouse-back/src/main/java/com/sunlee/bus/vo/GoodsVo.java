package com.sunlee.bus.vo;

import com.sunlee.bus.entity.Goods;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: sunlee
 * @Date: 2026/04/01 22:30
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GoodsVo extends Goods {

    private Integer page=1;
    private Integer limit=10;

    /**
     * 按仓库筛选：传 warehouseId 时，返回的库存为该仓分仓库存（无记录按0计）
     */
    private Integer warehouseId;

}
