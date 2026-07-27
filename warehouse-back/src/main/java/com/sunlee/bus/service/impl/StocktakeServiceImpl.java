package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.Goods;
import com.sunlee.bus.entity.Stocktake;
import com.sunlee.bus.entity.StocktakeItem;
import com.sunlee.bus.mapper.GoodsMapper;
import com.sunlee.bus.mapper.GoodsStockMapper;
import com.sunlee.bus.mapper.StocktakeMapper;
import com.sunlee.bus.service.IGoodsService;
import com.sunlee.bus.service.IGoodsStockService;
import com.sunlee.bus.service.IStocktakeItemService;
import com.sunlee.bus.service.IStocktakeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@Transactional
public class StocktakeServiceImpl extends ServiceImpl<StocktakeMapper, Stocktake> implements IStocktakeService {

    @Autowired
    private IGoodsService goodsService;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private IStocktakeItemService stocktakeItemService;

    @Autowired
    private IGoodsStockService goodsStockService;

    @Autowired
    private GoodsStockMapper goodsStockMapper;

    @Override
    public Stocktake createStocktake(String operator, String remark, Integer warehouseId) {
        Integer wid = goodsStockService.resolveWarehouseId(warehouseId);
        // 生成盘点单号: ST + 日期 + 序号
        String no = "ST" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        Stocktake stocktake = new Stocktake();
        stocktake.setStocktakeNo(no);
        stocktake.setStatus(0);
        stocktake.setOperator(operator);
        stocktake.setRemark(remark);
        stocktake.setWarehouseId(wid);
        stocktake.setCreateTime(new Date());
        save(stocktake);

        // 自动加载该仓所有商品当前分仓库存作为盘点明细（无库存记录的商品按0计）
        List<java.util.Map<String, Object>> goodsStockList = goodsStockMapper.selectWarehouseGoodsStock(wid);
        for (java.util.Map<String, Object> row : goodsStockList) {
            StocktakeItem item = new StocktakeItem();
            item.setStocktakeId(stocktake.getId());
            item.setGoodsid(((Number) row.get("goodsid")).intValue());
            item.setGoodsname((String) row.get("goodsname"));
            item.setSystemNum(((Number) row.get("number")).intValue());
            item.setActualNum(null); // 待填写
            item.setDiffNum(null);
            stocktakeItemService.save(item);
        }
        stocktake.setItems(stocktakeItemService.loadByStocktakeId(stocktake.getId()));
        return stocktake;
    }

    @Override
    public void submitStocktake(Integer stocktakeId) {
        Stocktake stocktake = getById(stocktakeId);
        if (stocktake == null || stocktake.getStatus() != 0) {
            throw new RuntimeException("盘点单不存在或状态异常");
        }

        List<StocktakeItem> items = stocktakeItemService.loadByStocktakeId(stocktakeId);
        for (StocktakeItem item : items) {
            if (item.getActualNum() == null) {
                throw new RuntimeException("商品 [" + item.getGoodsname() + "] 尚未填写实际盘点数量");
            }
        }

        // 原子认领盘点单（status 0→1），防止并发重复提交导致差异被重复应用
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Stocktake> claim =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        claim.eq("id", stocktakeId).eq("status", 0)
                .set("status", 1).set("finish_time", new Date());
        if (baseMapper.update(null, claim) == 0) {
            throw new RuntimeException("盘点单已被提交，请勿重复操作");
        }

        for (StocktakeItem item : items) {
            // 计算差异
            item.setDiffNum(item.getActualNum() - item.getSystemNum());
            stocktakeItemService.updateById(item);

            // 按差异量原子调整该仓分仓库存（而非盲写实际数量）：
            // 盘点单创建到提交之间发生的正常出入库不会被覆盖，只有差异部分被修正
            int diff = item.getDiffNum();
            if (diff > 0) {
                goodsStockService.increase(item.getGoodsid(), stocktake.getWarehouseId(), diff);
            } else if (diff < 0) {
                goodsStockService.decrease(item.getGoodsid(), stocktake.getWarehouseId(), -diff, item.getGoodsname());
            }
        }
    }

    @Override
    public void cancelStocktake(Integer stocktakeId) {
        Stocktake stocktake = getById(stocktakeId);
        if (stocktake == null || stocktake.getStatus() != 0) {
            throw new RuntimeException("盘点单不存在或状态异常");
        }
        stocktake.setStatus(2);
        updateById(stocktake);
    }
}
