package com.sunlee.bus.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sunlee.bus.entity.SurplusOrder;
import com.sunlee.bus.service.ISurplusOrderService;
import com.sunlee.bus.vo.SurplusOrderVo;
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
@RequestMapping("/surplus")
public class SurplusOrderController {

    @Autowired
    private ISurplusOrderService surplusOrderService;

    @RequestMapping("loadAllSurplus")
    public DataGridView loadAllSurplus(SurplusOrderVo vo) {
        IPage<SurplusOrder> page = surplusOrderService.pageOrders(vo);
        return new DataGridView(page.getTotal(), page.getRecords());
    }

    @RequestMapping("loadSurplusDetail")
    public Map<String, Object> loadSurplusDetail(Long id) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "ok");
            map.put("data", surplusOrderService.getDetail(id));
        } catch (Exception e) {
            log.error("查询升益单失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @RequestMapping("loadWarehouseBatches")
    public Map<String, Object> loadWarehouseBatches(Long warehouseId) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "ok");
            map.put("data", surplusOrderService.loadWarehouseBatches(warehouseId));
        } catch (Exception e) {
            log.error("载入批号库存失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "添加", module = "批号升益", description = "'保存升益单: ' + #args[0].orderNo")
    @RequestMapping("saveSurplus")
    public Map<String, Object> saveSurplus(@RequestBody SurplusOrderVo vo) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "保存成功");
            map.put("data", surplusOrderService.saveDraft(vo));
        } catch (Exception e) {
            log.error("保存升益单失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "确认", module = "批号升益", description = "'确认升益单ID: ' + #args[0]")
    @RequestMapping("confirmSurplus")
    public ResultObj confirmSurplus(Long id) {
        try {
            surplusOrderService.confirm(id);
            return ResultObj.ok("确认成功，已按批号调整库存");
        } catch (Exception e) {
            log.error("确认升益单失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }

    @OperationLog(type = "删除", module = "批号升益", description = "'删除升益单ID: ' + #args[0]")
    @RequestMapping("deleteSurplus")
    public ResultObj deleteSurplus(Long id) {
        try {
            surplusOrderService.deleteDraft(id);
            return ResultObj.DELETE_SUCCESS;
        } catch (Exception e) {
            log.error("删除升益单失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }
}
