package com.sunlee.bus.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.PurchaseOrder;
import com.sunlee.bus.vo.PurchaseInvoiceLineVo;
import com.sunlee.bus.vo.PurchaseOrderVo;

import java.util.Map;

public interface IPurchaseOrderService extends IService<PurchaseOrder> {

    IPage<PurchaseOrder> pageOrders(PurchaseOrderVo vo);

    IPage<PurchaseInvoiceLineVo> pageInvoiceLines(PurchaseOrderVo vo);

    Map<String, Object> sumInvoiceLines(PurchaseOrderVo vo);

    PurchaseOrder getDetail(Long id);

    /** 按采购单号或供应商发票号定位（全电号、后段顺序号） */
    PurchaseOrder findByQuery(String keyword);

    PurchaseOrder saveDraft(PurchaseOrderVo vo);

    void confirm(Long id);

    void deleteDraft(Long id);

    String previewSupplierInvoiceNo(Long supplierId, java.time.LocalDate bizDate);

    PurchaseOrder receiveSupplierInvoice(Long id);

    /** 补齐模拟采购合同、发票、随货同行单；replace=true 时覆盖已有模拟件 */
    void ensureSimulatedDocs(Long id, boolean replace);
}
