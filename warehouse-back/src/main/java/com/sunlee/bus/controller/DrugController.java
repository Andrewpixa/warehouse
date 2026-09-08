package com.sunlee.bus.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sunlee.bus.common.PharmaIds;
import com.sunlee.bus.entity.Drug;
import com.sunlee.bus.service.IDrugService;
import com.sunlee.bus.vo.DrugVo;
import com.sunlee.sys.annotation.OperationLog;
import com.sunlee.sys.common.Constast;
import com.sunlee.sys.common.DataGridView;
import com.sunlee.sys.common.ResultObj;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/drug")
public class DrugController {

    @Autowired
    private IDrugService drugService;

    @RequestMapping("loadAllDrug")
    public DataGridView loadAllDrug(DrugVo drugVo) {
        IPage<Drug> page = new Page<>(drugVo.getPage(), drugVo.getLimit());
        QueryWrapper<Drug> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(drugVo.getCode()), "code", drugVo.getCode());
        queryWrapper.like(StringUtils.isNotBlank(drugVo.getGenericName()), "generic_name", drugVo.getGenericName());
        queryWrapper.like(StringUtils.isNotBlank(drugVo.getTradeName()), "trade_name", drugVo.getTradeName());
        queryWrapper.like(StringUtils.isNotBlank(drugVo.getApprovalNo()), "approval_no", drugVo.getApprovalNo());
        queryWrapper.eq(drugVo.getStatus() != null, "status", drugVo.getStatus());
        if (PharmaIds.CATEGORY_DEVICE.equals(drugVo.getCategory())) {
            queryWrapper.ge("id", PharmaIds.DEVICE_START).le("id", PharmaIds.DEVICE_END);
        } else if (PharmaIds.CATEGORY_DRUG.equals(drugVo.getCategory())) {
            queryWrapper.ge("id", PharmaIds.DRUG_START).le("id", PharmaIds.DRUG_END);
        }
        queryWrapper.orderByDesc("id");
        drugService.page(page, queryWrapper);
        page.getRecords().forEach(this::fillCategory);
        return new DataGridView(page.getTotal(), page.getRecords());
    }

    @RequestMapping("loadAllDrugForSelect")
    public DataGridView loadAllDrugForSelect() {
        QueryWrapper<Drug> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", Constast.AVAILABLE_TRUE);
        queryWrapper.orderByAsc("id");
        List<Drug> list = drugService.list(queryWrapper);
        list.forEach(this::fillCategory);
        return new DataGridView(list);
    }

    @OperationLog(type = "添加", module = "药品管理", description = "'添加药品: ' + #args[0].genericName")
    @RequestMapping("addDrug")
    public ResultObj addDrug(DrugVo drugVo) {
        try {
            if (drugVo.getStatus() == null) {
                drugVo.setStatus(Constast.AVAILABLE_TRUE);
            }
            if (drugVo.getIsColdChain() == null) {
                drugVo.setIsColdChain(0);
            }
            drugVo.setId(null);
            drugService.save(drugVo);
            return ResultObj.ADD_SUCCESS;
        } catch (Exception e) {
            log.error("添加药品失败: {}", e.getMessage(), e);
            return ResultObj.error("添加失败: " + e.getMessage());
        }
    }

    @OperationLog(type = "修改", module = "药品管理", description = "'修改药品: ' + #args[0].genericName")
    @RequestMapping("updateDrug")
    public ResultObj updateDrug(DrugVo drugVo) {
        try {
            drugService.updateById(drugVo);
            return ResultObj.UPDATE_SUCCESS;
        } catch (Exception e) {
            log.error("修改药品失败: {}", e.getMessage(), e);
            return ResultObj.error("修改失败: " + e.getMessage());
        }
    }

    @OperationLog(type = "删除", module = "药品管理", description = "'删除药品ID: ' + #args[0]")
    @RequestMapping("deleteDrug")
    public ResultObj deleteDrug(Long id) {
        try {
            drugService.removeById(id);
            return ResultObj.DELETE_SUCCESS;
        } catch (Exception e) {
            log.error("删除药品失败: {}", e.getMessage(), e);
            return ResultObj.error("删除失败: " + e.getMessage());
        }
    }

    private void fillCategory(Drug drug) {
        if (drug != null && StringUtils.isBlank(drug.getCategory())) {
            drug.setCategory(PharmaIds.categoryOf(drug.getId()));
        }
    }
}
