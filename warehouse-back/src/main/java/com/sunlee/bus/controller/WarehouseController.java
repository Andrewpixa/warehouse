package com.sunlee.bus.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sunlee.bus.entity.Warehouse;
import com.sunlee.bus.service.IWarehouseService;
import com.sunlee.bus.vo.WarehouseVo;
import com.sunlee.sys.annotation.OperationLog;
import com.sunlee.sys.common.Constast;
import com.sunlee.sys.common.DataGridView;
import com.sunlee.sys.common.ResultObj;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 仓库档案控制器（pharma_ims.warehouses）
 * 库位/分仓库存等旧接口本阶段返回空数据，避免拖垮档案页。
 */
@Slf4j
@RestController
@RequestMapping("/warehouse")
public class WarehouseController {

    @Autowired
    private IWarehouseService warehouseService;

    @RequestMapping("loadAllWarehouse")
    public DataGridView loadAllWarehouse(WarehouseVo warehouseVo) {
        IPage<Warehouse> page = new Page<>(warehouseVo.getPage(), warehouseVo.getLimit());
        QueryWrapper<Warehouse> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(warehouseVo.getName()), "name", warehouseVo.getName());
        queryWrapper.like(StringUtils.isNotBlank(warehouseVo.getCode()), "code", warehouseVo.getCode());
        queryWrapper.eq(StringUtils.isNotBlank(warehouseVo.getWhType()), "wh_type", warehouseVo.getWhType());
        queryWrapper.eq(warehouseVo.getStatus() != null, "status", warehouseVo.getStatus());
        queryWrapper.orderByAsc("id");
        warehouseService.page(page, queryWrapper);
        return new DataGridView(page.getTotal(), page.getRecords());
    }

    @RequestMapping("loadAllWarehouseForSelect")
    public DataGridView loadAllWarehouseForSelect() {
        QueryWrapper<Warehouse> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", Constast.AVAILABLE_TRUE);
        queryWrapper.orderByAsc("id");
        List<Warehouse> list = warehouseService.list(queryWrapper);
        return new DataGridView(list);
    }

    @OperationLog(type = "添加", module = "仓库管理", description = "'添加仓库: ' + #args[0].name")
    @RequestMapping("addWarehouse")
    public ResultObj addWarehouse(Warehouse warehouse) {
        try {
            if (warehouse.getStatus() == null) {
                warehouse.setStatus(Constast.AVAILABLE_TRUE);
            }
            warehouseService.save(warehouse);
            return ResultObj.ADD_SUCCESS;
        } catch (Exception e) {
            log.error("添加仓库失败: {}", e.getMessage(), e);
            return ResultObj.error("添加失败: " + e.getMessage());
        }
    }

    @OperationLog(type = "修改", module = "仓库管理", description = "'修改仓库: ' + #args[0].name")
    @RequestMapping("updateWarehouse")
    public ResultObj updateWarehouse(Warehouse warehouse) {
        try {
            warehouseService.updateById(warehouse);
            return ResultObj.UPDATE_SUCCESS;
        } catch (Exception e) {
            log.error("修改仓库失败: {}", e.getMessage(), e);
            return ResultObj.error("修改失败: " + e.getMessage());
        }
    }

    @OperationLog(type = "删除", module = "仓库管理", description = "'删除仓库ID: ' + #args[0]")
    @RequestMapping("deleteWarehouse")
    public ResultObj deleteWarehouse(Long id) {
        try {
            warehouseService.deleteWarehouse(id);
            return ResultObj.DELETE_SUCCESS;
        } catch (Exception e) {
            log.error("删除仓库失败: {}", e.getMessage(), e);
            return ResultObj.error("删除失败: " + e.getMessage());
        }
    }

    // ===== 旧扩展接口占位（本阶段未迁移）=====

    @RequestMapping("setDefaultWarehouse")
    public ResultObj setDefaultWarehouse(Long id) {
        return ResultObj.error("药企档案阶段暂不支持默认仓设置");
    }

    @RequestMapping("loadLocations")
    public DataGridView loadLocations(Long warehouseId) {
        return new DataGridView(new ArrayList<>());
    }

    @RequestMapping("addLocation")
    public ResultObj addLocation() {
        return ResultObj.error("药企档案阶段暂未启用库位管理");
    }

    @RequestMapping("updateLocation")
    public ResultObj updateLocation() {
        return ResultObj.error("药企档案阶段暂未启用库位管理");
    }

    @RequestMapping("deleteLocation")
    public ResultObj deleteLocation(Long id) {
        return ResultObj.error("药企档案阶段暂未启用库位管理");
    }

    @RequestMapping("loadGoodsStock")
    public DataGridView loadGoodsStock() {
        return new DataGridView(0L, new ArrayList<>());
    }

    @RequestMapping("loadStockByGoodsId")
    public DataGridView loadStockByGoodsId(Long goodsid) {
        return new DataGridView(new ArrayList<>());
    }

    @RequestMapping("loadWarehouseWarnings")
    public DataGridView loadWarehouseWarnings() {
        return new DataGridView(new ArrayList<>());
    }

    @RequestMapping("loadWarnRules")
    public DataGridView loadWarnRules(Long warehouseId) {
        return new DataGridView(new ArrayList<>());
    }

    @RequestMapping("saveWarnRule")
    public ResultObj saveWarnRule() {
        return ResultObj.error("药企档案阶段暂未启用分仓预警");
    }

    @RequestMapping("deleteWarnRule")
    public ResultObj deleteWarnRule(Long id) {
        return ResultObj.error("药企档案阶段暂未启用分仓预警");
    }
}
