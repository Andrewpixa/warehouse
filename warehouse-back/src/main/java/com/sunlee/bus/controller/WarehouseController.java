package com.sunlee.bus.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sunlee.bus.entity.GoodsStock;
import com.sunlee.bus.entity.Warehouse;
import com.sunlee.bus.entity.WarehouseLocation;
import com.sunlee.bus.entity.WarehouseWarnRule;
import com.sunlee.bus.mapper.GoodsStockMapper;
import com.sunlee.bus.service.IWarehouseLocationService;
import com.sunlee.bus.service.IWarehouseService;
import com.sunlee.bus.service.IWarehouseWarnRuleService;
import com.sunlee.bus.vo.GoodsStockVo;
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

import java.util.List;

/**
 * 仓库/库位/分仓库存/分仓预警规则 控制器
 *
 * @author sunlee
 * @since 2026-07-26
 */
@Slf4j
@RestController
@RequestMapping("/warehouse")
public class WarehouseController {

    @Autowired
    private IWarehouseService warehouseService;

    @Autowired
    private IWarehouseLocationService warehouseLocationService;

    @Autowired
    private IWarehouseWarnRuleService warehouseWarnRuleService;

    @Autowired
    private GoodsStockMapper goodsStockMapper;

    // ==================== 仓库 ====================

    @RequestMapping("loadAllWarehouse")
    public DataGridView loadAllWarehouse(WarehouseVo warehouseVo) {
        IPage<Warehouse> page = new Page<>(warehouseVo.getPage(), warehouseVo.getLimit());
        QueryWrapper<Warehouse> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(warehouseVo.getName()), "name", warehouseVo.getName());
        queryWrapper.orderByAsc("id");
        warehouseService.page(page, queryWrapper);
        return new DataGridView(page.getTotal(), page.getRecords());
    }

    @RequestMapping("loadAllWarehouseForSelect")
    public DataGridView loadAllWarehouseForSelect() {
        QueryWrapper<Warehouse> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("available", Constast.AVAILABLE_TRUE);
        queryWrapper.orderByAsc("id");
        List<Warehouse> list = warehouseService.list(queryWrapper);
        return new DataGridView(list);
    }

    @OperationLog(type = "添加", module = "仓库管理", description = "'添加仓库: ' + #args[0].name")
    @RequestMapping("addWarehouse")
    public ResultObj addWarehouse(Warehouse warehouse) {
        try {
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

    @OperationLog(type = "修改", module = "仓库管理", description = "'设置默认仓ID: ' + #args[0]")
    @RequestMapping("setDefaultWarehouse")
    public ResultObj setDefaultWarehouse(Integer id) {
        try {
            warehouseService.setDefault(id);
            return ResultObj.UPDATE_SUCCESS;
        } catch (Exception e) {
            log.error("设置默认仓失败: {}", e.getMessage(), e);
            return ResultObj.error("设置失败: " + e.getMessage());
        }
    }

    @OperationLog(type = "删除", module = "仓库管理", description = "'删除仓库ID: ' + #args[0]")
    @RequestMapping("deleteWarehouse")
    public ResultObj deleteWarehouse(Integer id) {
        try {
            warehouseService.deleteWarehouse(id);
            return ResultObj.DELETE_SUCCESS;
        } catch (Exception e) {
            log.error("删除仓库失败: {}", e.getMessage(), e);
            return ResultObj.error("删除失败: " + e.getMessage());
        }
    }

    // ==================== 库位 ====================

    @RequestMapping("loadLocations")
    public DataGridView loadLocations(Integer warehouseId) {
        if (warehouseId == null) {
            return new DataGridView(new java.util.ArrayList<>());
        }
        QueryWrapper<WarehouseLocation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("warehouse_id", warehouseId);
        queryWrapper.orderByAsc("zone", "code");
        List<WarehouseLocation> list = warehouseLocationService.list(queryWrapper);
        return new DataGridView(list);
    }

    @OperationLog(type = "添加", module = "库位管理", description = "'添加库位: ' + #args[0].code")
    @RequestMapping("addLocation")
    public ResultObj addLocation(WarehouseLocation location) {
        try {
            if (location.getWarehouseId() == null) {
                return ResultObj.error("所属仓库不能为空");
            }
            warehouseLocationService.save(location);
            return ResultObj.ADD_SUCCESS;
        } catch (Exception e) {
            log.error("添加库位失败: {}", e.getMessage(), e);
            return ResultObj.error("添加失败: " + e.getMessage());
        }
    }

    @OperationLog(type = "修改", module = "库位管理", description = "'修改库位: ' + #args[0].code")
    @RequestMapping("updateLocation")
    public ResultObj updateLocation(WarehouseLocation location) {
        try {
            warehouseLocationService.updateById(location);
            return ResultObj.UPDATE_SUCCESS;
        } catch (Exception e) {
            log.error("修改库位失败: {}", e.getMessage(), e);
            return ResultObj.error("修改失败: " + e.getMessage());
        }
    }

    @OperationLog(type = "删除", module = "库位管理", description = "'删除库位ID: ' + #args[0]")
    @RequestMapping("deleteLocation")
    public ResultObj deleteLocation(Integer id) {
        try {
            warehouseLocationService.removeById(id);
            return ResultObj.DELETE_SUCCESS;
        } catch (Exception e) {
            log.error("删除库位失败: {}", e.getMessage(), e);
            return ResultObj.error("删除失败: " + e.getMessage());
        }
    }

    // ==================== 分仓库存 ====================

    @RequestMapping("loadGoodsStock")
    public DataGridView loadGoodsStock(GoodsStockVo vo) {
        Page<GoodsStock> page = new Page<>(
                vo.getPage() != null ? vo.getPage() : 1,
                vo.getLimit() != null ? vo.getLimit() : 10);
        IPage<GoodsStock> result = goodsStockMapper.selectStockPage(page, vo.getWarehouseId(), vo.getGoodsname());
        return new DataGridView(result.getTotal(), result.getRecords());
    }

    @RequestMapping("loadStockByGoodsId")
    public DataGridView loadStockByGoodsId(Integer goodsid) {
        return new DataGridView(goodsStockMapper.selectByGoodsId(goodsid));
    }

    // ==================== 分仓预警 ====================

    @RequestMapping("loadWarehouseWarnings")
    public DataGridView loadWarehouseWarnings() {
        return new DataGridView(goodsStockMapper.selectWarehouseWarnings());
    }

    @RequestMapping("loadWarnRules")
    public DataGridView loadWarnRules(Integer warehouseId) {
        QueryWrapper<WarehouseWarnRule> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(warehouseId != null, "warehouse_id", warehouseId);
        queryWrapper.orderByAsc("warehouse_id", "goodsid");
        List<WarehouseWarnRule> list = warehouseWarnRuleService.list(queryWrapper);
        return new DataGridView(list);
    }

    @OperationLog(type = "修改", module = "分仓预警", description = "'保存分仓预警规则: 商品' + #args[0].goodsid + ' 仓库' + #args[0].warehouseId")
    @RequestMapping("saveWarnRule")
    public ResultObj saveWarnRule(WarehouseWarnRule rule) {
        try {
            warehouseWarnRuleService.saveRule(rule);
            return ResultObj.UPDATE_SUCCESS;
        } catch (Exception e) {
            log.error("保存分仓预警规则失败: {}", e.getMessage(), e);
            return ResultObj.error("保存失败: " + e.getMessage());
        }
    }

    @OperationLog(type = "删除", module = "分仓预警", description = "'删除分仓预警规则ID: ' + #args[0]")
    @RequestMapping("deleteWarnRule")
    public ResultObj deleteWarnRule(Integer id) {
        try {
            warehouseWarnRuleService.removeById(id);
            return ResultObj.DELETE_SUCCESS;
        } catch (Exception e) {
            log.error("删除分仓预警规则失败: {}", e.getMessage(), e);
            return ResultObj.error("删除失败: " + e.getMessage());
        }
    }
}
