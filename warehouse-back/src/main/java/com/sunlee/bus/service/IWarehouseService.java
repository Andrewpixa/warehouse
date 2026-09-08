package com.sunlee.bus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.Warehouse;

public interface IWarehouseService extends IService<Warehouse> {

    void deleteWarehouse(Long id);
}
