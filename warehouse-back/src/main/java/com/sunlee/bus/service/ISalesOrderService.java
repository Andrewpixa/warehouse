package com.sunlee.bus.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.SalesOrder;
import com.sunlee.bus.vo.SalesOrderVo;

import java.math.BigDecimal;

public interface ISalesOrderService extends IService<SalesOrder> {

    IPage<SalesOrder> pageOrders(SalesOrderVo vo);

    SalesOrder getDetail(Long id);

    SalesOrder getByInvoiceNo(String invoiceNo);

    SalesOrder saveDraft(SalesOrderVo vo);

    void confirm(Long id);

    void confirm(Long id, String shipTime, String einvoiceNo, String einvoicePath);

    IPage<SalesOrder> pagePendingReceipt(SalesOrderVo vo);

    void confirmReceipt(SalesOrderVo vo);

    void deleteDraft(Long id);

    IPage<SalesOrder> pageUnpaid(SalesOrderVo vo);

    void markPaid(Long id, String paidStatus, BigDecimal paidAmount);

    SalesOrder saveReversal(SalesOrderVo vo);

    java.util.List<SalesOrder> listReversals(String originalInvoiceNo);
}
