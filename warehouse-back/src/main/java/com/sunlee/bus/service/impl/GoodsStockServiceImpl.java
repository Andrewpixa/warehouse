package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.GoodsStock;
import com.sunlee.bus.entity.Warehouse;
import com.sunlee.bus.mapper.GoodsMapper;
import com.sunlee.bus.mapper.GoodsStockMapper;
import com.sunlee.bus.mapper.WarehouseMapper;
import com.sunlee.bus.service.IGoodsStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 分仓库存服务实现
 *
 * @author sunlee
 * @since 2026-07-26
 */
@Service
@Transactional
public class GoodsStockServiceImpl extends ServiceImpl<GoodsStockMapper, GoodsStock> implements IGoodsStockService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Override
    public Integer resolveWarehouseId(Integer warehouseId) {
        if (warehouseId != null) {
            return warehouseId;
        }
        QueryWrapper<Warehouse> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("id");
        queryWrapper.last("limit 1");
        Warehouse defaultWarehouse = warehouseMapper.selectOne(queryWrapper);
        // 药企库无默认仓字段时取第一个启用仓库；无数据时兜底 1
        return defaultWarehouse != null ? defaultWarehouse.getId().intValue() : 1;
    }

    @Override
    public void increase(Integer goodsid, Integer warehouseId, Integer number) {
        if (number == null || number <= 0) {
            throw new RuntimeException("库存变动数量必须大于0");
        }
        Integer wid = resolveWarehouseId(warehouseId);
        // 原子增加分仓库存（upsert），再同步总库存缓存
        baseMapper.increaseStock(goodsid, wid, number);
        goodsMapper.increaseStock(goodsid, number);
    }

    @Override
    public void decrease(Integer goodsid, Integer warehouseId, Integer number, String goodsname) {
        if (number == null || number <= 0) {
            throw new RuntimeException("库存变动数量必须大于0");
        }
        Integer wid = resolveWarehouseId(warehouseId);
        // 原子扣减分仓库存（带库存充足守卫），不足时抛异常回滚
        if (baseMapper.decreaseStock(goodsid, wid, number) == 0) {
            throw new RuntimeException("商品【" + goodsname + "】该仓库存不足，当前库存: " + getStockNumber(goodsid, wid));
        }
        // 同步总库存缓存。分仓扣减已成功时总量必然充足；返回0说明总缓存与分仓之和已不一致，抛错回滚暴露问题
        if (goodsMapper.decreaseStock(goodsid, number) == 0) {
            throw new RuntimeException("商品【" + goodsname + "】总库存缓存不足，账实异常，请联系管理员核查");
        }
    }

    @Override
    public Integer getStockNumber(Integer goodsid, Integer warehouseId) {
        QueryWrapper<GoodsStock> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("goodsid", goodsid);
        queryWrapper.eq("warehouse_id", resolveWarehouseId(warehouseId));
        GoodsStock stock = baseMapper.selectOne(queryWrapper);
        return stock != null && stock.getNumber() != null ? stock.getNumber() : 0;
    }
}
