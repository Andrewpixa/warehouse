package com.sunlee.bus.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.BatchStock;
import com.sunlee.bus.vo.BatchStockVo;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface IBatchStockService extends IService<BatchStock> {

    IPage<BatchStock> pageStock(BatchStockVo vo);

    void increase(Long drugId, Long warehouseId, String batchNo, String qualityStatus,
                  BigDecimal qty, LocalDate productionDate, LocalDate expireDate);

    void decrease(Long drugId, Long warehouseId, String batchNo, String qualityStatus, BigDecimal qty);
}
