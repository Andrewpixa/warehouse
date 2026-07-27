package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.GoodsStock;
import com.sunlee.bus.entity.Warehouse;
import com.sunlee.bus.mapper.GoodsStockMapper;
import com.sunlee.bus.mapper.WarehouseMapper;
import com.sunlee.bus.service.IWarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 仓库服务实现
 *
 * @author sunlee
 * @since 2026-07-26
 */
@Service
@Transactional
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, Warehouse> implements IWarehouseService {

    @Autowired
    private GoodsStockMapper goodsStockMapper;

    @Override
    public boolean save(Warehouse entity) {
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(new Date());
        }
        boolean result = super.save(entity);
        // 设为默认仓时清空其他默认标记
        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            baseMapper.clearDefault(entity.getId());
        }
        return result;
    }

    @Override
    public boolean updateById(Warehouse entity) {
        boolean result = super.updateById(entity);
        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            baseMapper.clearDefault(entity.getId());
        }
        return result;
    }

    @Override
    public void setDefault(Integer id) {
        Warehouse warehouse = getById(id);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在: " + id);
        }
        baseMapper.clearDefault(id);
        Warehouse update = new Warehouse();
        update.setId(id);
        update.setIsDefault(1);
        updateById(update);
    }

    @Override
    public void deleteWarehouse(Integer id) {
        Warehouse warehouse = getById(id);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在: " + id);
        }
        if (warehouse.getIsDefault() != null && warehouse.getIsDefault() == 1) {
            throw new RuntimeException("默认仓库不能删除，请先设置其他仓库为默认仓");
        }
        // 尚有库存的仓库禁止删除，防止库存记录成为无主数据
        QueryWrapper<GoodsStock> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("warehouse_id", id);
        queryWrapper.gt("number", 0);
        if (goodsStockMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("该仓库尚有库存，请先调拨清空后再删除");
        }
        removeById(id);
    }
}
