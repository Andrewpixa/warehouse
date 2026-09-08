package com.sunlee.bus.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sunlee.bus.entity.SalesOrder;
import com.sunlee.bus.service.ISalesOrderService;
import com.sunlee.bus.service.ITraceCodeService;
import com.sunlee.bus.vo.SalesOrderVo;
import com.sunlee.sys.annotation.OperationLog;
import com.sunlee.sys.common.Constast;
import com.sunlee.sys.common.DataGridView;
import com.sunlee.sys.common.ResultObj;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/outbound")
public class SalesOutboundController {

    @Autowired
    private ISalesOrderService salesOrderService;

    @Autowired
    private ITraceCodeService traceCodeService;

    @RequestMapping("loadAllOutbound")
    public DataGridView loadAllOutbound(SalesOrderVo vo) {
        IPage<SalesOrder> page = salesOrderService.pageOrders(vo);
        return new DataGridView(page.getTotal(), page.getRecords());
    }

    @RequestMapping("loadOutboundDetail")
    public Map<String, Object> loadOutboundDetail(Long id) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "ok");
            map.put("data", salesOrderService.getDetail(id));
        } catch (Exception e) {
            log.error("查询出库单失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @RequestMapping("loadReceiptDetail")
    public Map<String, Object> loadReceiptDetail(Long id) {
        return loadOutboundDetail(id);
    }

    @RequestMapping("loadByInvoice")
    public Map<String, Object> loadByInvoice(String invoiceNo) {
        Map<String, Object> map = new HashMap<>();
        try {
            SalesOrder order = salesOrderService.getByInvoiceNo(invoiceNo);
            map.put("code", Constast.OK);
            map.put("msg", "ok");
            map.put("data", order);
            map.put("traces", traceCodeService.listByInvoiceNo(invoiceNo));
            String originalNo = order.getInvoiceNo();
            if ("红冲".equals(order.getOrderType()) && order.getOriginalInvoiceNo() != null) {
                originalNo = order.getOriginalInvoiceNo();
            }
            map.put("reversals", salesOrderService.listReversals(originalNo));
        } catch (Exception e) {
            log.error("按发票号查询失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "添加", module = "出库单", description = "'保存出库单: ' + #args[0].orderNo")
    @RequestMapping("saveOutbound")
    public Map<String, Object> saveOutbound(@RequestBody SalesOrderVo vo) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "保存成功");
            map.put("data", salesOrderService.saveDraft(vo));
        } catch (Exception e) {
            log.error("保存出库单失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "确认", module = "出库单", description = "'确认出库单ID: ' + #args[0]")
    @RequestMapping("confirmOutbound")
    public ResultObj confirmOutbound(Long id, String shipTime, String einvoiceNo, String einvoicePath) {
        try {
            salesOrderService.confirm(id, shipTime, einvoiceNo, einvoicePath);
            SalesOrder order = salesOrderService.getById(id);
            if (order != null && "红冲".equals(order.getOrderType())) {
                return ResultObj.ok("确认成功，已加回批号库存");
            }
            return ResultObj.ok("已发货，库存已扣减，等待医院确认收货");
        } catch (Exception e) {
            log.error("确认出库单失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }

    @RequestMapping("loadPendingReceipt")
    public DataGridView loadPendingReceipt(SalesOrderVo vo) {
        IPage<SalesOrder> page = salesOrderService.pagePendingReceipt(vo);
        return new DataGridView(page.getTotal(), page.getRecords());
    }

    @OperationLog(type = "确认", module = "医院收货", description = "'医院收货 出库单ID: ' + #args[0].id")
    @RequestMapping("confirmReceipt")
    public ResultObj confirmReceipt(@RequestBody SalesOrderVo vo) {
        try {
            salesOrderService.confirmReceipt(vo);
            return ResultObj.ok("收货确认已提交");
        } catch (Exception e) {
            log.error("确认收货失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }

    @OperationLog(type = "删除", module = "出库单", description = "'删除出库单ID: ' + #args[0]")
    @RequestMapping("deleteOutbound")
    public ResultObj deleteOutbound(Long id) {
        try {
            salesOrderService.deleteDraft(id);
            return ResultObj.DELETE_SUCCESS;
        } catch (Exception e) {
            log.error("删除出库单失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }

    @RequestMapping("loadUnpaidOutbound")
    public DataGridView loadUnpaidOutbound(SalesOrderVo vo) {
        IPage<SalesOrder> page = salesOrderService.pageUnpaid(vo);
        return new DataGridView(page.getTotal(), page.getRecords());
    }

    @OperationLog(type = "修改", module = "出库单", description = "'标记回款 出库单ID: ' + #args[0]")
    @RequestMapping("markPaid")
    public ResultObj markPaid(Long id, String paidStatus, java.math.BigDecimal paidAmount) {
        try {
            salesOrderService.markPaid(id, paidStatus, paidAmount);
            return ResultObj.ok("回款状态已更新");
        } catch (Exception e) {
            log.error("标记回款失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }

    @OperationLog(type = "添加", module = "出库单", description = "'保存红冲单 原发票: ' + #args[0].originalInvoiceNo")
    @RequestMapping("saveReversal")
    public Map<String, Object> saveReversal(@RequestBody SalesOrderVo vo) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "红冲草稿已保存");
            map.put("data", salesOrderService.saveReversal(vo));
        } catch (Exception e) {
            log.error("保存红冲单失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @RequestMapping("loadReversals")
    public DataGridView loadReversals(String originalInvoiceNo) {
        return new DataGridView(salesOrderService.listReversals(originalInvoiceNo));
    }
}
