package com.sunlee.bus.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.SurplusOrder;
import com.sunlee.bus.entity.SurplusOrderItem;
import com.sunlee.bus.vo.SurplusOrderVo;

import java.util.List;

public interface ISurplusOrderService extends IService<SurplusOrder> {

    IPage<SurplusOrder> pageOrders(SurplusOrderVo vo);

    SurplusOrder getDetail(Long id);

    SurplusOrder saveDraft(SurplusOrderVo vo);

    void confirm(Long id);

    void deleteDraft(Long id);

    List<SurplusOrderItem> loadWarehouseBatches(Long warehouseId);
}
