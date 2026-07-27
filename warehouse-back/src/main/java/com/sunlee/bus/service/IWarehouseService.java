package com.sunlee.bus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.Warehouse;

/**
 * 仓库服务
 *
 * @author sunlee
 * @since 2026-07-26
 */
public interface IWarehouseService extends IService<Warehouse> {

    /**
     * 设置默认仓（全系统唯一，其余仓库自动取消默认）
     */
    void setDefault(Integer id);

    /**
     * 删除仓库（默认仓或尚有库存的仓库禁止删除）
     */
    void deleteWarehouse(Integer id);
}
