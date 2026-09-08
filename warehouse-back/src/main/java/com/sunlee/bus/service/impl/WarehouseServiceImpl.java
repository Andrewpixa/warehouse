package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.Warehouse;
import com.sunlee.bus.mapper.WarehouseMapper;
import com.sunlee.bus.service.IWarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, Warehouse> implements IWarehouseService {

    @Override
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = getById(id);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在: " + id);
        }
        removeById(id);
    }
}
