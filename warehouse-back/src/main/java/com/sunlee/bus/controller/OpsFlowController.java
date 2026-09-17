package com.sunlee.bus.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sunlee.bus.entity.OpsFlowDoc;
import com.sunlee.bus.service.IOpsFlowService;
import com.sunlee.bus.vo.OpsFlowDocVo;
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
@RequestMapping("/ops")
public class OpsFlowController {

    @Autowired
    private IOpsFlowService opsFlowService;

    @RequestMapping("loadAll")
    public DataGridView loadAll(OpsFlowDocVo vo) {
        IPage<OpsFlowDoc> page = opsFlowService.pageDocs(vo);
        return new DataGridView(page.getTotal(), page.getRecords());
    }

    @RequestMapping("loadDetail")
    public Map<String, Object> loadDetail(Long id) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "ok");
            map.put("data", opsFlowService.getDetail(id));
        } catch (Exception e) {
            log.error("查询运营单据失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "添加", module = "运营协同", description = "'保存运营单: ' + #args[0].docNo")
    @RequestMapping("save")
    public Map<String, Object> save(@RequestBody OpsFlowDocVo vo) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "保存成功");
            map.put("data", opsFlowService.saveDraft(vo));
        } catch (Exception e) {
            log.error("保存运营单失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "确认", module = "运营协同", description = "'推进运营单ID: ' + #args[0]")
    @RequestMapping("confirm")
    public ResultObj confirm(Long id, String action) {
        try {
            opsFlowService.confirm(id, action);
            return ResultObj.ok("已推进");
        } catch (Exception e) {
            log.error("推进运营单失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }

    @OperationLog(type = "删除", module = "运营协同", description = "'删除运营单ID: ' + #args[0]")
    @RequestMapping("delete")
    public ResultObj delete(Long id) {
        try {
            opsFlowService.deleteDraft(id);
            return ResultObj.DELETE_SUCCESS;
        } catch (Exception e) {
            log.error("删除运营单失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }

    @RequestMapping("workbench")
    public Map<String, Object> workbench() {
        Map<String, Object> map = new HashMap<>();
        map.put("code", Constast.OK);
        map.put("data", opsFlowService.workbench());
        return map;
    }

    @RequestMapping("printPack")
    public Map<String, Object> printPack(String invoiceNo) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("data", opsFlowService.printPack(invoiceNo));
        } catch (Exception e) {
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @RequestMapping("makerAccounts")
    public DataGridView makerAccounts() {
        var rows = opsFlowService.makerAccounts();
        return new DataGridView((long) rows.size(), rows);
    }

    @RequestMapping("makerQuery")
    public Map<String, Object> makerQuery(String loginName, String password, String from, String to) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("data", opsFlowService.makerQuery(loginName, password, from, to));
        } catch (Exception e) {
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }
}
