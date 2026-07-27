package com.sunlee.bus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.GoodsStock;

/**
 * 分仓库存服务：所有出入库/退货/盘点/调拨的库存变动统一入口。
 * 双写规则：先原子改 bus_goods_stock（账实源头），同事务同步 bus_goods.number 总库存缓存。
 *
 * @author sunlee
 * @since 2026-07-26
 */
public interface IGoodsStockService extends IService<GoodsStock> {

    /**
     * 解析仓库ID：未指定时落到默认仓
     */
    Integer resolveWarehouseId(Integer warehouseId);

    /**
     * 增加分仓库存（无记录时插入），并同步总库存缓存
     * @param goodsid 商品id
     * @param warehouseId 仓库id（null 时落默认仓）
     * @param number 增加数量（必须>0）
     */
    void increase(Integer goodsid, Integer warehouseId, Integer number);

    /**
     * 扣减分仓库存（该仓不足时抛异常回滚），并同步总库存缓存
     * @param goodsid 商品id
     * @param warehouseId 仓库id（null 时落默认仓）
     * @param number 扣减数量（必须>0）
     * @param goodsname 商品名（仅用于异常消息）
     */
    void decrease(Integer goodsid, Integer warehouseId, Integer number, String goodsname);

    /**
     * 查询某商品在某仓的库存数量（无记录返回0）
     */
    Integer getStockNumber(Integer goodsid, Integer warehouseId);
}
