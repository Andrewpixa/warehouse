package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.common.PharmaNos;
import com.sunlee.bus.entity.BatchStock;
import com.sunlee.bus.entity.Drug;
import com.sunlee.bus.entity.PurchaseOrderItem;
import com.sunlee.bus.entity.SurplusOrder;
import com.sunlee.bus.entity.SurplusOrderItem;
import com.sunlee.bus.entity.Warehouse;
import com.sunlee.bus.mapper.SurplusOrderMapper;
import com.sunlee.bus.service.IBatchStockService;
import com.sunlee.bus.service.IDrugService;
import com.sunlee.bus.service.IPurchaseOrderItemService;
import com.sunlee.bus.service.ISurplusOrderItemService;
import com.sunlee.bus.service.ISurplusOrderService;
import com.sunlee.bus.service.IWarehouseService;
import com.sunlee.bus.vo.SurplusOrderVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class SurplusOrderServiceImpl extends ServiceImpl<SurplusOrderMapper, SurplusOrder>
        implements ISurplusOrderService {

    @Autowired
    private ISurplusOrderItemService itemService;

    @Autowired
    private IBatchStockService batchStockService;

    @Autowired
    private IWarehouseService warehouseService;

    @Autowired
    private IDrugService drugService;

    @Autowired
    private IPurchaseOrderItemService purchaseOrderItemService;

    @Override
    public IPage<SurplusOrder> pageOrders(SurplusOrderVo vo) {
        IPage<SurplusOrder> page = new Page<>(vo.getPage(), vo.getLimit());
        QueryWrapper<SurplusOrder> qw = new QueryWrapper<>();
        qw.like(StringUtils.isNotBlank(vo.getOrderNo()), "order_no", vo.getOrderNo());
        qw.eq(vo.getWarehouseId() != null, "warehouse_id", vo.getWarehouseId());
        qw.eq(StringUtils.isNotBlank(vo.getStatus()), "status", vo.getStatus());
        qw.orderByDesc("id");
        this.page(page, qw);
        page.getRecords().forEach(this::fillHeaderNames);
        return page;
    }

    @Override
    public SurplusOrder getDetail(Long id) {
        SurplusOrder order = this.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("升益单不存在");
        }
        fillHeaderNames(order);
        order.setItems(loadItems(id));
        return order;
    }

    @Override
    @Transactional
    public SurplusOrder saveDraft(SurplusOrderVo vo) {
        List<SurplusOrderItem> items = vo.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("请至少添加一行批号明细");
        }
        if (vo.getWarehouseId() == null) {
            throw new IllegalArgumentException("请选择仓库");
        }
        if (vo.getBizDate() == null) {
            throw new IllegalArgumentException("请选择业务日期");
        }

        SurplusOrder order;
        if (vo.getId() == null) {
            order = new SurplusOrder();
            order.setOrderNo(nextOrderNo());
            order.setStatus(PharmaNos.STATUS_DRAFT);
        } else {
            order = this.getById(vo.getId());
            if (order == null) {
                throw new IllegalArgumentException("升益单不存在");
            }
            assertDraft(order);
            QueryWrapper<SurplusOrderItem> delQw = new QueryWrapper<>();
            delQw.eq("order_id", order.getId());
            itemService.remove(delQw);
        }

        order.setWarehouseId(vo.getWarehouseId());
        order.setBizDate(vo.getBizDate());
        order.setRemark(vo.getRemark());

        BigDecimal surplusQty = BigDecimal.ZERO;
        BigDecimal surplusAmt = BigDecimal.ZERO;
        BigDecimal lossQty = BigDecimal.ZERO;
        BigDecimal lossAmt = BigDecimal.ZERO;
        for (SurplusOrderItem item : items) {
            normalizeItem(item);
            surplusQty = surplusQty.add(item.getSurplusQty());
            surplusAmt = surplusAmt.add(item.getSurplusAmount());
            lossQty = lossQty.add(item.getLossQty());
            lossAmt = lossAmt.add(item.getLossAmount());
        }
        order.setTotalSurplusQty(surplusQty);
        order.setTotalSurplusAmount(surplusAmt);
        order.setTotalLossQty(lossQty);
        order.setTotalLossAmount(lossAmt);

        if (order.getId() == null) {
            this.save(order);
        } else {
            this.updateById(order);
        }

        for (SurplusOrderItem item : items) {
            item.setId(null);
            item.setOrderId(order.getId());
            itemService.save(item);
        }
        return getDetail(order.getId());
    }

    @Override
    @Transactional
    public void confirm(Long id) {
        SurplusOrder order = this.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("升益单不存在");
        }
        assertDraft(order);
        List<SurplusOrderItem> items = loadItems(id);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("升益单没有明细，无法确认");
        }
        boolean hasDiff = false;
        for (SurplusOrderItem item : items) {
            normalizeItem(item);
            if (item.getSurplusQty().compareTo(BigDecimal.ZERO) > 0) {
                hasDiff = true;
                batchStockService.increase(
                        item.getDrugId(),
                        order.getWarehouseId(),
                        item.getBatchNo(),
                        item.getQualityStatus(),
                        item.getSurplusQty(),
                        item.getProductionDate(),
                        item.getExpireDate()
                );
            }
            if (item.getLossQty().compareTo(BigDecimal.ZERO) > 0) {
                hasDiff = true;
                batchStockService.decrease(
                        item.getDrugId(),
                        order.getWarehouseId(),
                        item.getBatchNo(),
                        item.getQualityStatus(),
                        item.getLossQty()
                );
            }
        }
        if (!hasDiff) {
            throw new IllegalArgumentException("实盘与账面全部一致，没有升益或损耗，无需确认");
        }
        order.setStatus(PharmaNos.STATUS_CONFIRMED);
        order.setConfirmedAt(new Date());
        this.updateById(order);
    }

    @Override
    @Transactional
    public void deleteDraft(Long id) {
        SurplusOrder order = this.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("升益单不存在");
        }
        assertDraft(order);
        QueryWrapper<SurplusOrderItem> delQw = new QueryWrapper<>();
        delQw.eq("order_id", id);
        itemService.remove(delQw);
        this.removeById(id);
    }

    @Override
    public List<SurplusOrderItem> loadWarehouseBatches(Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("请选择仓库");
        }
        QueryWrapper<BatchStock> qw = new QueryWrapper<>();
        qw.eq("warehouse_id", warehouseId);
        qw.orderByAsc("drug_id", "batch_no");
        List<BatchStock> stocks = batchStockService.list(qw);
        List<SurplusOrderItem> items = new ArrayList<>();
        for (BatchStock stock : stocks) {
            SurplusOrderItem item = new SurplusOrderItem();
            item.setDrugId(stock.getDrugId());
            item.setBatchNo(stock.getBatchNo());
            item.setQualityStatus(StringUtils.defaultIfBlank(stock.getQualityStatus(), PharmaNos.QUALITY_OK));
            item.setBookQty(nz(stock.getQty()));
            item.setActualQty(nz(stock.getQty()));
            item.setProductionDate(stock.getProductionDate());
            item.setExpireDate(stock.getExpireDate());
            item.setUnitCost(resolveUnitCost(stock.getDrugId(), stock.getBatchNo()));
            normalizeItem(item);
            fillDrugName(item);
            items.add(item);
        }
        return items;
    }

    private void assertDraft(SurplusOrder order) {
        if (!PharmaNos.STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("只有草稿单据可以修改或删除");
        }
    }

    private String nextOrderNo() {
        for (int i = 0; i < 5; i++) {
            String no = PharmaNos.orderNo("SY");
            QueryWrapper<SurplusOrder> qw = new QueryWrapper<>();
            qw.eq("order_no", no);
            if (this.count(qw) == 0) {
                return no;
            }
        }
        return PharmaNos.orderNo("SY") + Thread.currentThread().getId();
    }

    private void normalizeItem(SurplusOrderItem item) {
        if (item.getDrugId() == null) {
            throw new IllegalArgumentException("明细必须选择药品");
        }
        if (StringUtils.isBlank(item.getBatchNo())) {
            throw new IllegalArgumentException("明细必须填写批号");
        }
        if (StringUtils.isBlank(item.getQualityStatus())) {
            item.setQualityStatus(PharmaNos.QUALITY_OK);
        }
        item.setBookQty(nz(item.getBookQty()));
        if (item.getActualQty() == null) {
            throw new IllegalArgumentException("批号 " + item.getBatchNo() + " 请填写实盘数量");
        }
        if (item.getActualQty().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("批号 " + item.getBatchNo() + " 实盘数量不能为负");
        }
        if (item.getUnitCost() == null) {
            item.setUnitCost(resolveUnitCost(item.getDrugId(), item.getBatchNo()));
        }
        if (item.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("批号 " + item.getBatchNo() + " 成本单价不能为负");
        }

        BigDecimal diff = item.getActualQty().subtract(item.getBookQty());
        BigDecimal surplusQty = diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
        BigDecimal lossQty = diff.compareTo(BigDecimal.ZERO) < 0 ? diff.negate() : BigDecimal.ZERO;
        item.setSurplusQty(surplusQty);
        item.setLossQty(lossQty);
        item.setSurplusAmount(money(surplusQty.multiply(item.getUnitCost())));
        item.setLossAmount(money(lossQty.multiply(item.getUnitCost())));
    }

    private BigDecimal resolveUnitCost(Long drugId, String batchNo) {
        QueryWrapper<PurchaseOrderItem> qw = new QueryWrapper<>();
        qw.eq("drug_id", drugId);
        qw.eq("batch_no", batchNo);
        qw.isNotNull("purchase_price");
        qw.gt("purchase_price", 0);
        qw.orderByDesc("id");
        qw.last("LIMIT 1");
        PurchaseOrderItem purchase = purchaseOrderItemService.getOne(qw, false);
        if (purchase != null && purchase.getPurchasePrice() != null) {
            return purchase.getPurchasePrice();
        }
        Drug drug = drugService.getById(drugId);
        if (drug != null && drug.getRefPurchasePrice() != null) {
            return drug.getRefPurchasePrice();
        }
        return BigDecimal.ZERO;
    }

    private List<SurplusOrderItem> loadItems(Long orderId) {
        QueryWrapper<SurplusOrderItem> qw = new QueryWrapper<>();
        qw.eq("order_id", orderId);
        qw.orderByAsc("id");
        List<SurplusOrderItem> items = itemService.list(qw);
        items.forEach(this::fillDrugName);
        return items;
    }

    private void fillDrugName(SurplusOrderItem item) {
        if (item.getDrugId() == null) {
            return;
        }
        Drug drug = drugService.getById(item.getDrugId());
        if (drug != null) {
            item.setDrugName(drug.getGenericName());
            item.setDrugSpec(drug.getSpec());
        }
    }

    private void fillHeaderNames(SurplusOrder order) {
        if (order.getWarehouseId() == null) {
            return;
        }
        Warehouse warehouse = warehouseService.getById(order.getWarehouseId());
        if (warehouse != null) {
            order.setWarehouseName(warehouse.getName());
        }
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
