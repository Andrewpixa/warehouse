package com.sunlee.bus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sunlee.bus.entity.GoodsStock;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 分仓库存 Mapper 接口
 *
 * @author sunlee
 * @since 2026-07-26
 */
public interface GoodsStockMapper extends BaseMapper<GoodsStock> {

    /**
     * 原子增加分仓库存（无记录时插入，有记录时累加）
     * @return 受影响行数
     */
    int increaseStock(@Param("goodsid") Integer goodsid, @Param("warehouseId") Integer warehouseId,
                      @Param("number") Integer number);

    /**
     * 原子扣减分仓库存（该仓库存不足时返回0，不会扣成负数）
     * @return 受影响行数，0表示该仓库存不足
     */
    int decreaseStock(@Param("goodsid") Integer goodsid, @Param("warehouseId") Integer warehouseId,
                      @Param("number") Integer number);

    /**
     * 分仓库存分页查询（联商品与仓库）
     */
    IPage<GoodsStock> selectStockPage(Page<GoodsStock> page, @Param("warehouseId") Integer warehouseId,
                                      @Param("goodsname") String goodsname);

    /**
     * 查询某商品在各仓的库存分布
     */
    List<GoodsStock> selectByGoodsId(@Param("goodsid") Integer goodsid);

    /**
     * 分仓预警查询：仅对有分仓预警规则的商品，该仓库存低于规则阈值时返回
     */
    List<GoodsStock> selectWarehouseWarnings();

    /**
     * 查询某仓全部商品库存（盘点单创建用，LEFT JOIN 保证无库存记录的商品也在列）
     */
    List<Map<String, Object>> selectWarehouseGoodsStock(@Param("warehouseId") Integer warehouseId);
}
