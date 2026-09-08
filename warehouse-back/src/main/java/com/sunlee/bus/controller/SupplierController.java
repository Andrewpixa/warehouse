package com.sunlee.bus.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sunlee.bus.entity.Supplier;
import com.sunlee.bus.service.ISupplierService;
import com.sunlee.bus.vo.SupplierVo;
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
@RequestMapping("/supplier")
public class SupplierController {

    @Autowired
    private ISupplierService supplierService;

    @RequestMapping("loadAllSupplier")
    public DataGridView loadAllSupplier(SupplierVo supplierVo) {
        IPage<Supplier> page = new Page<>(supplierVo.getPage(), supplierVo.getLimit());
        QueryWrapper<Supplier> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(supplierVo.getCode()), "code", supplierVo.getCode());
        queryWrapper.like(StringUtils.isNotBlank(supplierVo.getName()), "name", supplierVo.getName());
        queryWrapper.like(StringUtils.isNotBlank(supplierVo.getContact()), "contact", supplierVo.getContact());
        queryWrapper.like(StringUtils.isNotBlank(supplierVo.getPhone()), "phone", supplierVo.getPhone());
        queryWrapper.eq(supplierVo.getStatus() != null, "status", supplierVo.getStatus());
        queryWrapper.orderByDesc("id");
        supplierService.page(page, queryWrapper);
        return new DataGridView(page.getTotal(), page.getRecords());
    }

    @RequestMapping("loadAllSupplierForSelect")
    public DataGridView loadAllSupplierForSelect() {
        QueryWrapper<Supplier> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", Constast.AVAILABLE_TRUE);
        queryWrapper.orderByAsc("id");
        List<Supplier> list = supplierService.list(queryWrapper);
        return new DataGridView(list);
    }

    @OperationLog(type = "添加", module = "供应商管理", description = "'添加供应商: ' + #args[0].name")
    @RequestMapping("addSupplier")
    public ResultObj addSupplier(SupplierVo supplierVo) {
        try {
            if (supplierVo.getStatus() == null) {
                supplierVo.setStatus(Constast.AVAILABLE_TRUE);
            }
            if (supplierVo.getFirstCampOk() == null) {
                supplierVo.setFirstCampOk(0);
            }
            supplierVo.setId(null);
            supplierService.save(supplierVo);
            return ResultObj.ADD_SUCCESS;
        } catch (Exception e) {
            log.error("添加供应商失败: {}", e.getMessage(), e);
            return ResultObj.error("添加失败: " + e.getMessage());
        }
    }

    @OperationLog(type = "修改", module = "供应商管理", description = "'修改供应商: ' + #args[0].name")
    @RequestMapping("updateSupplier")
    public ResultObj updateSupplier(SupplierVo supplierVo) {
        try {
            supplierService.updateById(supplierVo);
            return ResultObj.UPDATE_SUCCESS;
        } catch (Exception e) {
            log.error("修改供应商失败: {}", e.getMessage(), e);
            return ResultObj.error("修改失败: " + e.getMessage());
        }
    }

    @OperationLog(type = "删除", module = "供应商管理", description = "'删除供应商ID: ' + #args[0]")
    @RequestMapping("deleteSupplier")
    public ResultObj deleteSupplier(Long id) {
        try {
            supplierService.removeById(id);
            return ResultObj.DELETE_SUCCESS;
        } catch (Exception e) {
            log.error("删除供应商失败: {}", e.getMessage(), e);
            return ResultObj.error("删除失败: " + e.getMessage());
        }
    }
}
