package com.sunlee.bus.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.PurchaseOrder;
import com.sunlee.bus.vo.PurchaseOrderVo;

public interface IPurchaseOrderService extends IService<PurchaseOrder> {

    IPage<PurchaseOrder> pageOrders(PurchaseOrderVo vo);

    PurchaseOrder getDetail(Long id);

    PurchaseOrder saveDraft(PurchaseOrderVo vo);

    void confirm(Long id);

    void deleteDraft(Long id);
}
