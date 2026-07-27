package com.sunlee.bus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sunlee.bus.entity.Warehouse;
import org.apache.ibatis.annotations.Param;

/**
 * 仓库 Mapper 接口
 *
 * @author sunlee
 * @since 2026-07-26
 */
public interface WarehouseMapper extends BaseMapper<Warehouse> {

    /**
     * 将其他仓库全部设为非默认（设置默认仓前先清空）
     * @param excludeId 排除的仓库id
     */
    void clearDefault(@Param("excludeId") Integer excludeId);
}
