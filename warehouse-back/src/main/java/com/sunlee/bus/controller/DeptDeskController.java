package com.sunlee.bus.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sunlee.bus.common.PharmaNos;
import com.sunlee.bus.entity.BatchStock;
import com.sunlee.bus.entity.PurchaseOrder;
import com.sunlee.bus.entity.SalesOrder;
import com.sunlee.bus.entity.Supplier;
import com.sunlee.bus.service.IBatchStockService;
import com.sunlee.bus.service.IOpsFlowService;
import com.sunlee.bus.service.IPurchaseOrderService;
import com.sunlee.bus.service.ISalesOrderService;
import com.sunlee.bus.service.ISupplierService;
import com.sunlee.sys.common.Constast;
import com.sunlee.sys.entity.Loginfo;
import com.sunlee.sys.entity.User;
import com.sunlee.sys.service.ILoginfoService;
import com.sunlee.sys.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/desk")
public class DeptDeskController {

    @Autowired
    private IOpsFlowService opsFlowService;
    @Autowired
    private IPurchaseOrderService purchaseOrderService;
    @Autowired
    private ISalesOrderService salesOrderService;
    @Autowired
    private ISupplierService supplierService;
    @Autowired
    private IBatchStockService batchStockService;
    @Autowired
    private IUserService userService;
    @Autowired
    private ILoginfoService loginfoService;

    @RequestMapping("summary")
    public Map<String, Object> summary() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", Constast.OK);
        try {
            Map<String, Object> wb = opsFlowService.workbench();
            QueryWrapper<PurchaseOrder> pd = new QueryWrapper<PurchaseOrder>().eq("status", PharmaNos.STATUS_DRAFT);
            QueryWrapper<SalesOrder> sd = new QueryWrapper<SalesOrder>().eq("status", PharmaNos.STATUS_DRAFT);
            QueryWrapper<Supplier> camp = new QueryWrapper<Supplier>().and(w -> w.eq("first_camp_ok", 0).or().isNull("first_camp_ok"));
            QueryWrapper<BatchStock> q = new QueryWrapper<BatchStock>().ne("quality_status", "合格")
                    .gt("qty", BigDecimal.ZERO);
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date today = cal.getTime();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ops", wb);
            data.put("draftPurchase", purchaseOrderService.count(pd));
            data.put("draftOutbound", salesOrderService.count(sd));
            data.put("pendingFirstCamp", supplierService.count(camp));
            data.put("abnormalBatch", batchStockService.count(q));
            data.put("userCount", userService.count(new QueryWrapper<User>().eq("available", 1)));
            data.put("todayLogin", loginfoService.count(new QueryWrapper<Loginfo>().ge("logintime", today)));
            map.put("data", data);
        } catch (Exception e) {
            log.error("部门作业台汇总失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }
}
