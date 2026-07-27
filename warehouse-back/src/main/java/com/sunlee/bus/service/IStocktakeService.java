package com.sunlee.bus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.Stocktake;

public interface IStocktakeService extends IService<Stocktake> {

    /**
     * 创建盘点单（按仓加载该仓当前库存）
     * @param warehouseId 盘点仓库（null 时落默认仓）
     */
    Stocktake createStocktake(String operator, String remark, Integer warehouseId);

    /**
     * 提交盘点结果（更新库存）
     */
    void submitStocktake(Integer stocktakeId);

    /**
     * 取消盘点单
     */
    void cancelStocktake(Integer stocktakeId);
}
