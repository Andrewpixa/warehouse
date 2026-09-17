package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.BatchStock;
import com.sunlee.bus.entity.Drug;
import com.sunlee.bus.entity.Warehouse;
import com.sunlee.bus.mapper.BatchStockMapper;
import com.sunlee.bus.service.IBatchStockService;
import com.sunlee.bus.service.IDrugService;
import com.sunlee.bus.service.IWarehouseService;
import com.sunlee.bus.vo.BatchStockVo;
import com.sunlee.sys.common.RedisBiz;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BatchStockServiceImpl extends ServiceImpl<BatchStockMapper, BatchStock> implements IBatchStockService {

    @Autowired
    private IDrugService drugService;

    @Autowired
    private IWarehouseService warehouseService;

    @Autowired
    private RedisBiz redisBiz;

    @Override
    public IPage<BatchStock> pageStock(BatchStockVo vo) {
        IPage<BatchStock> page = new Page<>(vo.getPage(), vo.getLimit());
        QueryWrapper<BatchStock> qw = new QueryWrapper<>();
        qw.eq(vo.getDrugId() != null, "drug_id", vo.getDrugId());
        qw.eq(vo.getWarehouseId() != null, "warehouse_id", vo.getWarehouseId());
        qw.like(StringUtils.isNotBlank(vo.getBatchNo()), "batch_no", vo.getBatchNo());
        if ("非合格".equals(vo.getQualityStatus())) {
            qw.ne("quality_status", "合格");
            qw.gt("qty", java.math.BigDecimal.ZERO);
        } else {
            qw.eq(StringUtils.isNotBlank(vo.getQualityStatus()), "quality_status", vo.getQualityStatus());
        }
        if ("near".equals(vo.getExpireFilter())) {
            int days = vo.getNearExpireDays() == null ? 90 : vo.getNearExpireDays();
            qw.ge("expire_date", LocalDate.now());
            qw.le("expire_date", LocalDate.now().plusDays(days));
        } else if ("expired".equals(vo.getExpireFilter())) {
            qw.lt("expire_date", LocalDate.now());
        }
        if (StringUtils.isNotBlank(vo.getDrugName())) {
            QueryWrapper<Drug> drugQw = new QueryWrapper<>();
            drugQw.and(w -> w.like("generic_name", vo.getDrugName())
                    .or().like("trade_name", vo.getDrugName())
                    .or().like("code", vo.getDrugName())
                    .or().eq("id", vo.getDrugName()));
            List<Long> drugIds = drugService.list(drugQw).stream().map(Drug::getId).collect(Collectors.toList());
            if (drugIds.isEmpty()) {
                page.setTotal(0);
                return page;
            }
            qw.in("drug_id", drugIds);
        }
        qw.orderByDesc("id");
        this.page(page, qw);
        page.getRecords().forEach(this::fillNames);
        return page;
    }

    @Override
    @Transactional
    public void increase(Long drugId, Long warehouseId, String batchNo, String qualityStatus,
                         BigDecimal qty, LocalDate productionDate, LocalDate expireDate) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("入库数量必须大于 0");
        }
        String quality = StringUtils.defaultIfBlank(qualityStatus, "合格");
        BatchStock stock = findOne(drugId, warehouseId, batchNo, quality);
        Date now = new Date();
        if (stock == null) {
            stock = new BatchStock();
            stock.setDrugId(drugId);
            stock.setWarehouseId(warehouseId);
            stock.setBatchNo(batchNo);
            stock.setQualityStatus(quality);
            stock.setQty(qty);
            stock.setProductionDate(productionDate);
            stock.setExpireDate(expireDate);
            stock.setLastMoveAt(now);
            this.save(stock);
            return;
        }
        stock.setQty(stock.getQty().add(qty));
        if (productionDate != null) {
            stock.setProductionDate(productionDate);
        }
        if (expireDate != null) {
            stock.setExpireDate(expireDate);
        }
        stock.setLastMoveAt(now);
        this.updateById(stock);
    }

    @Override
    @Transactional
    public void decrease(Long drugId, Long warehouseId, String batchNo, String qualityStatus, BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("出库数量必须大于 0");
        }
        String quality = StringUtils.defaultIfBlank(qualityStatus, "合格");
        Drug drug = drugService.getById(drugId);
        String drugName = drug != null ? drug.getGenericName() : String.valueOf(drugId);
        String lockKey = "lock:stock:" + drugId + ":" + warehouseId + ":" + batchNo + ":" + quality;
        if (!redisBiz.tryLockWait(lockKey, Duration.ofSeconds(5), Duration.ofMillis(1500), Duration.ofMillis(50))) {
            throw new IllegalArgumentException("药品「" + drugName + "」批号 " + batchNo + " 正在出库，请稍后重试");
        }
        try {
            int rows = baseMapper.decreaseIfEnough(drugId, warehouseId, batchNo, quality, qty);
            if (rows > 0) {
                return;
            }
            BatchStock stock = findOne(drugId, warehouseId, batchNo, quality);
            if (stock == null || stock.getQty() == null) {
                throw new IllegalArgumentException("库存不足：药品「" + drugName + "」批号 " + batchNo + " 无库存");
            }
            throw new IllegalArgumentException("库存不足：药品「" + drugName + "」批号 " + batchNo
                    + " 现有 " + stock.getQty() + "，需要 " + qty);
        } finally {
            redisBiz.unlock(lockKey);
        }
    }

    @Override
    @Transactional
    public void changeQualityStatus(Long id, String qualityStatus) {
        if (id == null) {
            throw new IllegalArgumentException("批号库存ID必填");
        }
        String quality = StringUtils.trimToEmpty(qualityStatus);
        if (!("合格".equals(quality) || "待验".equals(quality) || "待复检".equals(quality)
                || "不合格".equals(quality) || "停售".equals(quality))) {
            throw new IllegalArgumentException("质量状态须为：合格 / 待验 / 待复检 / 不合格 / 停售");
        }
        BatchStock stock = this.getById(id);
        if (stock == null) {
            throw new IllegalArgumentException("批号库存不存在");
        }
        if (quality.equals(stock.getQualityStatus())) {
            return;
        }
        BatchStock existed = findOne(stock.getDrugId(), stock.getWarehouseId(), stock.getBatchNo(), quality);
        Date now = new Date();
        if (existed != null && !existed.getId().equals(stock.getId())) {
            BigDecimal add = stock.getQty() == null ? BigDecimal.ZERO : stock.getQty();
            existed.setQty((existed.getQty() == null ? BigDecimal.ZERO : existed.getQty()).add(add));
            existed.setLastMoveAt(now);
            this.updateById(existed);
            this.removeById(stock.getId());
            return;
        }
        stock.setQualityStatus(quality);
        stock.setLastMoveAt(now);
        this.updateById(stock);
    }

    private BatchStock findOne(Long drugId, Long warehouseId, String batchNo, String qualityStatus) {
        QueryWrapper<BatchStock> qw = new QueryWrapper<>();
        qw.eq("drug_id", drugId);
        qw.eq("warehouse_id", warehouseId);
        qw.eq("batch_no", batchNo);
        qw.eq("quality_status", qualityStatus);
        qw.last("LIMIT 1");
        return this.getOne(qw, false);
    }

    private void fillNames(BatchStock stock) {
        if (stock.getDrugId() != null) {
            Drug drug = drugService.getById(stock.getDrugId());
            if (drug != null) {
                stock.setDrugName(drug.getGenericName());
                stock.setDrugSpec(drug.getSpec());
            }
        }
        if (stock.getWarehouseId() != null) {
            Warehouse warehouse = warehouseService.getById(stock.getWarehouseId());
            if (warehouse != null) {
                stock.setWarehouseName(warehouse.getName());
            }
        }
    }
}
