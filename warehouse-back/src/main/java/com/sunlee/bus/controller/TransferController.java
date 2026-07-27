package com.sunlee.bus.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sunlee.bus.entity.Goods;
import com.sunlee.bus.entity.Transfer;
import com.sunlee.bus.entity.TransferItem;
import com.sunlee.bus.entity.Warehouse;
import com.sunlee.bus.mapper.GoodsMapper;
import com.sunlee.bus.mapper.TransferItemMapper;
import com.sunlee.bus.service.ITransferService;
import com.sunlee.bus.service.IWarehouseService;
import com.sunlee.bus.vo.TransferVo;
import com.sunlee.sys.annotation.OperationLog;
import com.sunlee.sys.common.DataGridView;
import com.sunlee.sys.common.ResultObj;
import com.sunlee.sys.common.WebUtils;
import com.sunlee.sys.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 库存调拨 控制器
 *
 * @author sunlee
 * @since 2026-07-26
 */
@Slf4j
@RestController
@RequestMapping("/transfer")
public class TransferController {

    @Autowired
    private ITransferService transferService;

    @Autowired
    private TransferItemMapper transferItemMapper;

    @Autowired
    private IWarehouseService warehouseService;

    @Autowired
    private GoodsMapper goodsMapper;

    @RequestMapping("loadAllTransfer")
    public DataGridView loadAllTransfer(TransferVo transferVo) {
        IPage<Transfer> page = new Page<>(transferVo.getPage(), transferVo.getLimit());
        QueryWrapper<Transfer> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(transferVo.getTransferNo()), "transfer_no", transferVo.getTransferNo());
        queryWrapper.eq(transferVo.getStatus() != null, "status", transferVo.getStatus());
        queryWrapper.orderByDesc("id");
        transferService.page(page, queryWrapper);
        // 填充仓库名称
        for (Transfer transfer : page.getRecords()) {
            Warehouse from = warehouseService.getById(transfer.getFromWarehouseId());
            Warehouse to = warehouseService.getById(transfer.getToWarehouseId());
            transfer.setFromWarehouseName(from != null ? from.getName() : String.valueOf(transfer.getFromWarehouseId()));
            transfer.setToWarehouseName(to != null ? to.getName() : String.valueOf(transfer.getToWarehouseId()));
        }
        return new DataGridView(page.getTotal(), page.getRecords());
    }

    @RequestMapping("loadTransferItems")
    public DataGridView loadTransferItems(Integer transferId) {
        QueryWrapper<TransferItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("transfer_id", transferId);
        List<TransferItem> list = transferItemMapper.selectList(queryWrapper);
        // 填充商品名称
        for (TransferItem item : list) {
            Goods goods = goodsMapper.selectById(item.getGoodsid());
            if (goods != null) {
                item.setGoodsname(goods.getGoodsname());
                item.setSize(goods.getSize());
            }
        }
        return new DataGridView(list);
    }

    @OperationLog(type = "添加", module = "库存调拨", description = "'创建调拨单: ' + #args[0].fromWarehouseId + ' -> ' + #args[0].toWarehouseId")
    @RequestMapping("createTransfer")
    public ResultObj createTransfer(@RequestBody Transfer transfer) {
        try {
            User user = (User) WebUtils.getSession().getAttribute("user");
            transfer.setOperator(user.getName());
            transferService.createTransfer(transfer);
            return ResultObj.ADD_SUCCESS;
        } catch (Exception e) {
            log.error("创建调拨单失败: {}", e.getMessage(), e);
            return ResultObj.error("创建失败: " + e.getMessage());
        }
    }

    @OperationLog(type = "修改", module = "库存调拨", description = "'发出调拨单ID: ' + #args[0]")
    @RequestMapping("shipTransfer")
    public ResultObj shipTransfer(Integer id) {
        try {
            transferService.shipTransfer(id);
            return ResultObj.ok("发出成功");
        } catch (Exception e) {
            log.error("发出调拨单失败: {}", e.getMessage(), e);
            return ResultObj.error("发出失败: " + e.getMessage());
        }
    }

    @OperationLog(type = "修改", module = "库存调拨", description = "'调拨收货ID: ' + #args[0]")
    @RequestMapping("receiveTransfer")
    public ResultObj receiveTransfer(Integer id) {
        try {
            transferService.receiveTransfer(id);
            return ResultObj.ok("收货成功");
        } catch (Exception e) {
            log.error("调拨收货失败: {}", e.getMessage(), e);
            return ResultObj.error("收货失败: " + e.getMessage());
        }
    }

    @OperationLog(type = "修改", module = "库存调拨", description = "'取消调拨单ID: ' + #args[0]")
    @RequestMapping("cancelTransfer")
    public ResultObj cancelTransfer(Integer id) {
        try {
            transferService.cancelTransfer(id);
            return ResultObj.CANCEL_SUCCESS;
        } catch (Exception e) {
            log.error("取消调拨单失败: {}", e.getMessage(), e);
            return ResultObj.error("取消失败: " + e.getMessage());
        }
    }

    @OperationLog(type = "删除", module = "库存调拨", description = "'删除调拨单ID: ' + #args[0]")
    @RequestMapping("deleteTransfer")
    public ResultObj deleteTransfer(Integer id) {
        try {
            transferService.deleteTransfer(id);
            return ResultObj.DELETE_SUCCESS;
        } catch (Exception e) {
            log.error("删除调拨单失败: {}", e.getMessage(), e);
            return ResultObj.error("删除失败: " + e.getMessage());
        }
    }
}
