package com.sunlee.bus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sunlee.bus.entity.BatchStock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface BatchStockMapper extends BaseMapper<BatchStock> {

    @Update("UPDATE batch_stocks SET qty = qty - #{qty}, last_move_at = NOW(), updated_at = NOW() "
            + "WHERE drug_id = #{drugId} AND warehouse_id = #{warehouseId} "
            + "AND batch_no = #{batchNo} AND quality_status = #{qualityStatus} AND qty >= #{qty}")
    int decreaseIfEnough(@Param("drugId") Long drugId,
                         @Param("warehouseId") Long warehouseId,
                         @Param("batchNo") String batchNo,
                         @Param("qualityStatus") String qualityStatus,
                         @Param("qty") BigDecimal qty);

    @Update("UPDATE batch_stocks SET qty = qty + #{qty}, last_move_at = NOW(), updated_at = NOW(), "
            + "production_date = COALESCE(#{productionDate}, production_date), "
            + "expire_date = COALESCE(#{expireDate}, expire_date) "
            + "WHERE drug_id = #{drugId} AND warehouse_id = #{warehouseId} "
            + "AND batch_no = #{batchNo} AND quality_status = #{qualityStatus}")
    int increaseQty(@Param("drugId") Long drugId,
                    @Param("warehouseId") Long warehouseId,
                    @Param("batchNo") String batchNo,
                    @Param("qualityStatus") String qualityStatus,
                    @Param("qty") BigDecimal qty,
                    @Param("productionDate") LocalDate productionDate,
                    @Param("expireDate") LocalDate expireDate);
}
