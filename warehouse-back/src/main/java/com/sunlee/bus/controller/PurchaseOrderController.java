package com.sunlee.bus.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sunlee.bus.entity.PurchaseOrder;
import com.sunlee.bus.service.IPurchaseOrderService;
import com.sunlee.bus.vo.PurchaseOrderVo;
import com.sunlee.sys.annotation.OperationLog;
import com.sunlee.sys.common.Constast;
import com.sunlee.sys.common.DataGridView;
import com.sunlee.sys.common.ResultObj;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/purchase")
public class PurchaseOrderController {

    @Autowired
    private IPurchaseOrderService purchaseOrderService;

    @RequestMapping("loadAllPurchase")
    public DataGridView loadAllPurchase(PurchaseOrderVo vo) {
        IPage<PurchaseOrder> page = purchaseOrderService.pageOrders(vo);
        return new DataGridView(page.getTotal(), page.getRecords());
    }

    @RequestMapping("loadPurchaseInvoiceLedger")
    public Map<String, Object> loadPurchaseInvoiceLedger(PurchaseOrderVo vo) {
        Map<String, Object> map = new HashMap<>();
        var page = purchaseOrderService.pageInvoiceLines(vo);
        map.put("code", 0);
        map.put("msg", "");
        map.put("count", page.getTotal());
        map.put("data", page.getRecords());
        map.put("summary", purchaseOrderService.sumInvoiceLines(vo));
        return map;
    }

    @RequestMapping("loadPurchaseDetail")
    public Map<String, Object> loadPurchaseDetail(Long id) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "ok");
            map.put("data", purchaseOrderService.getDetail(id));
        } catch (Exception e) {
            log.error("查询进货单失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @RequestMapping("previewSupplierInvoice")
    public Map<String, Object> previewSupplierInvoice(Long supplierId, String bizDate) {
        Map<String, Object> map = new HashMap<>();
        try {
            java.time.LocalDate d = StringUtils.isBlank(bizDate) ? java.time.LocalDate.now() : java.time.LocalDate.parse(bizDate);
            map.put("code", Constast.OK);
            map.put("invoiceNo", purchaseOrderService.previewSupplierInvoiceNo(supplierId, d));
        } catch (Exception e) {
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "修改", module = "进货单", description = "'模拟接收供应商发票 ID: ' + #args[0]")
    @RequestMapping("receiveSupplierInvoice")
    public Map<String, Object> receiveSupplierInvoice(Long id) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "已模拟生成采购合同、供应商发票和随货同行单，草稿仍可改发票号后重新接收");
            map.put("data", purchaseOrderService.receiveSupplierInvoice(id));
        } catch (Exception e) {
            log.error("模拟供应商发票失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "添加", module = "进货单", description = "'保存进货单: ' + #args[0].orderNo")
    @RequestMapping("savePurchase")
    public Map<String, Object> savePurchase(@RequestBody PurchaseOrderVo vo) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "保存成功");
            map.put("data", purchaseOrderService.saveDraft(vo));
        } catch (Exception e) {
            log.error("保存进货单失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "确认", module = "进货单", description = "'确认进货单ID: ' + #args[0]")
    @RequestMapping("confirmPurchase")
    public ResultObj confirmPurchase(Long id) {
        try {
            purchaseOrderService.confirm(id);
            return ResultObj.ok("确认成功，已更新批号库存");
        } catch (Exception e) {
            log.error("确认进货单失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }

    @OperationLog(type = "删除", module = "进货单", description = "'删除进货单ID: ' + #args[0]")
    @RequestMapping("deletePurchase")
    public ResultObj deletePurchase(Long id) {
        try {
            purchaseOrderService.deleteDraft(id);
            return ResultObj.DELETE_SUCCESS;
        } catch (Exception e) {
            log.error("删除进货单失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }
}
