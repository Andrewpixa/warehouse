package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.Goods;
import com.sunlee.bus.entity.Transfer;
import com.sunlee.bus.entity.TransferItem;
import com.sunlee.bus.mapper.GoodsMapper;
import com.sunlee.bus.mapper.TransferItemMapper;
import com.sunlee.bus.mapper.TransferMapper;
import com.sunlee.bus.service.IGoodsStockService;
import com.sunlee.bus.service.ITransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 库存调拨服务实现
 *
 * @author sunlee
 * @since 2026-07-26
 */
@Service
@Transactional
public class TransferServiceImpl extends ServiceImpl<TransferMapper, Transfer> implements ITransferService {

    @Autowired
    private TransferItemMapper transferItemMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private IGoodsStockService goodsStockService;

    @Override
    public void createTransfer(Transfer transfer) {
        if (transfer.getFromWarehouseId() == null || transfer.getToWarehouseId() == null) {
            throw new RuntimeException("调出仓与调入仓不能为空");
        }
        if (transfer.getFromWarehouseId().equals(transfer.getToWarehouseId())) {
            throw new RuntimeException("调出仓与调入仓不能相同");
        }
        List<TransferItem> items = transfer.getItems();
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("调拨明细不能为空");
        }
        for (TransferItem item : items) {
            if (item.getNumber() == null || item.getNumber() <= 0) {
                throw new RuntimeException("调拨数量必须大于0");
            }
            Goods goods = goodsMapper.selectById(item.getGoodsid());
            if (goods == null) {
                throw new RuntimeException("商品不存在: " + item.getGoodsid());
            }
        }
        // 生成调拨单号: TF + 时间戳 + 随机后缀（同 stocktake 单号风格，UUID 后缀防并发重复）
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        transfer.setTransferNo("TF" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + uuid);
        transfer.setStatus(0);
        transfer.setCreateTime(new Date());
        save(transfer);

        for (TransferItem item : items) {
            item.setId(null);
            item.setTransferId(transfer.getId());
            transferItemMapper.insert(item);
        }
    }

    @Override
    public void shipTransfer(Integer id) {
        Transfer transfer = getAndCheck(id, 0, "发出");
        List<TransferItem> items = loadItems(id);
        // 原子认领（status 0→1），防止并发重复发出导致源仓重复扣减
        UpdateWrapper<Transfer> claim = new UpdateWrapper<>();
        claim.eq("id", id).eq("status", 0)
                .set("status", 1).set("ship_time", new Date());
        if (baseMapper.update(null, claim) == 0) {
            throw new RuntimeException("调拨单状态已变更，请勿重复发出");
        }
        // 源仓原子扣减（任一商品不足则整单回滚）
        for (TransferItem item : items) {
            goodsStockService.decrease(item.getGoodsid(), transfer.getFromWarehouseId(),
                    item.getNumber(), goodsName(item.getGoodsid()));
        }
    }

    @Override
    public void receiveTransfer(Integer id) {
        Transfer transfer = getAndCheck(id, 1, "收货");
        List<TransferItem> items = loadItems(id);
        // 原子认领（status 1→2），防止并发重复收货导致目的仓重复入库
        UpdateWrapper<Transfer> claim = new UpdateWrapper<>();
        claim.eq("id", id).eq("status", 1)
                .set("status", 2).set("finish_time", new Date());
        if (baseMapper.update(null, claim) == 0) {
            throw new RuntimeException("调拨单状态已变更，请勿重复收货");
        }
        // 目的仓入库
        for (TransferItem item : items) {
            goodsStockService.increase(item.getGoodsid(), transfer.getToWarehouseId(), item.getNumber());
        }
    }

    @Override
    public void cancelTransfer(Integer id) {
        Transfer transfer = getById(id);
        if (transfer == null) {
            throw new RuntimeException("调拨单不存在: " + id);
        }
        if (transfer.getStatus() == 2) {
            throw new RuntimeException("已完成的调拨单不能取消");
        }
        if (transfer.getStatus() == 3) {
            throw new RuntimeException("调拨单已取消，请勿重复操作");
        }
        Integer fromStatus = transfer.getStatus();
        // 原子认领（当前状态→3）
        UpdateWrapper<Transfer> claim = new UpdateWrapper<>();
        claim.eq("id", id).eq("status", fromStatus).set("status", 3);
        if (baseMapper.update(null, claim) == 0) {
            throw new RuntimeException("调拨单状态已变更，请刷新后重试");
        }
        // 在途取消：源仓此前已扣减，需回补
        if (fromStatus == 1) {
            for (TransferItem item : loadItems(id)) {
                goodsStockService.increase(item.getGoodsid(), transfer.getFromWarehouseId(), item.getNumber());
            }
        }
    }

    @Override
    public void deleteTransfer(Integer id) {
        Transfer transfer = getById(id);
        if (transfer == null) {
            throw new RuntimeException("调拨单不存在: " + id);
        }
        if (transfer.getStatus() != 0 && transfer.getStatus() != 3) {
            throw new RuntimeException("仅草稿或已取消的调拨单可以删除");
        }
        QueryWrapper<TransferItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("transfer_id", id);
        transferItemMapper.delete(queryWrapper);
        removeById(id);
    }

    /**
     * 加载调拨单并校验当前状态
     */
    private Transfer getAndCheck(Integer id, int expectStatus, String action) {
        Transfer transfer = getById(id);
        if (transfer == null) {
            throw new RuntimeException("调拨单不存在: " + id);
        }
        if (transfer.getStatus() == null || transfer.getStatus() != expectStatus) {
            throw new RuntimeException("当前状态不允许" + action + "操作");
        }
        return transfer;
    }

    private List<TransferItem> loadItems(Integer transferId) {
        QueryWrapper<TransferItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("transfer_id", transferId);
        return transferItemMapper.selectList(queryWrapper);
    }

    private String goodsName(Integer goodsid) {
        Goods goods = goodsMapper.selectById(goodsid);
        return goods != null ? goods.getGoodsname() : String.valueOf(goodsid);
    }
}
