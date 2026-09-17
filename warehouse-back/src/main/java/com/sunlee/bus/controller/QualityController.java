package com.sunlee.bus.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sunlee.bus.entity.Supplier;
import com.sunlee.bus.service.IBatchStockService;
import com.sunlee.bus.service.ISupplierService;
import com.sunlee.bus.vo.BatchStockVo;
import com.sunlee.sys.annotation.OperationLog;
import com.sunlee.sys.common.DataGridView;
import com.sunlee.sys.common.ResultObj;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/quality")
public class QualityController {

    @Autowired
    private ISupplierService supplierService;
    @Autowired
    private IBatchStockService batchStockService;

    @RequestMapping("pendingSuppliers")
    public DataGridView pendingSuppliers(Integer page, Integer limit) {
        int p = page == null ? 1 : page;
        int l = limit == null ? 20 : limit;
        QueryWrapper<Supplier> qw = new QueryWrapper<>();
        qw.and(w -> w.eq("first_camp_ok", 0).or().isNull("first_camp_ok"));
        qw.orderByDesc("id");
        var result = supplierService.page(new Page<>(p, l), qw);
        return new DataGridView(result.getTotal(), result.getRecords());
    }

    @OperationLog(type = "修改", module = "质量", description = "'首营审核通过供应商ID: ' + #args[0]")
    @RequestMapping("approveFirstCamp")
    public ResultObj approveFirstCamp(Long id) {
        try {
            Supplier s = supplierService.getById(id);
            if (s == null) {
                return ResultObj.error("供应商不存在");
            }
            s.setFirstCampOk(1);
            supplierService.updateById(s);
            return ResultObj.ok("首营已通过");
        } catch (Exception e) {
            log.error("首营审核失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }

    @RequestMapping("pendingBatches")
    public DataGridView pendingBatches(BatchStockVo vo) {
        if (vo.getPage() == null) {
            vo.setPage(1);
        }
        if (vo.getLimit() == null) {
            vo.setLimit(20);
        }
        vo.setQualityStatus("非合格");
        var page = batchStockService.pageStock(vo);
        return new DataGridView(page.getTotal(), page.getRecords());
    }

    @OperationLog(type = "修改", module = "质量", description = "'批号质量状态 ID: ' + #args[0]")
    @RequestMapping("setBatchQuality")
    public ResultObj setBatchQuality(Long id, String qualityStatus) {
        try {
            batchStockService.changeQualityStatus(id, qualityStatus);
            return ResultObj.ok("质量状态已更新");
        } catch (Exception e) {
            log.error("改质量状态失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }
}
