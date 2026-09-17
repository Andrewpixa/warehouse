package com.sunlee.bus.service.impl;

import com.sunlee.bus.mapper.archive.ArchiveMapper;
import com.sunlee.bus.service.IDataArchiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务数据归档 Service 实现
 * <p>
 * 关键设计:
 * 1. 通过 @Lazy 自注入 IDataArchiveService,解决 Spring AOP 同类内调用失效问题,
 *    使 self.archiveXxxTable() 能触发 @Transactional(REQUIRES_NEW)
 * 2. 每张主表一个独立事务,主表 + 其明细表在同一事务内(原子性)
 * 3. 分页循环(LIMIT 500),避免长事务锁表
 * 4. 明细归档基于"归档主表 id"反查,与原主表是否已删无关
 */
@Slf4j
@Service
public class DataArchiveServiceImpl implements IDataArchiveService {

    @Autowired
    private ArchiveMapper archiveMapper;

    @Value("${archive.retention-days:365}")
    private Integer retentionDays;

    /**
     * 自注入以触发 AOP 代理,使 self.archiveXxxTable() 走 REQUIRES_NEW 事务
     */
    @Autowired
    @Lazy
    private IDataArchiveService self;

    private static final int PAGE_SIZE = 500;

    @Override
    public Map<String, Long> runArchive(Integer days) {
        int actualDays = resolveDays(days);
        Date cutoff = Date.from(Instant.now().minus(actualDays, ChronoUnit.DAYS));
        log.info("[数据归档] 开始执行,保留天数={}, cutoff={}", actualDays, cutoff);

        Map<String, Long> result = new LinkedHashMap<>();
        // 每张表独立事务,任一失败不影响其他表
        result.put("stocktake", safeArchive("stocktake", () -> self.archiveStocktakeTable(cutoff)));
        result.put("purchaseOrder", safeArchive("purchaseOrder", () -> self.archivePurchaseOrderTable(cutoff)));
        result.put("sales", safeArchive("sales", () -> self.archiveSalesTable(cutoff)));
        result.put("retail", safeArchive("retail", () -> self.archiveRetailTable(cutoff)));
        result.put("inport", safeArchive("inport", () -> self.archiveInportTable(cutoff)));
        result.put("outport", safeArchive("outport", () -> self.archiveOutportTable(cutoff)));

        log.info("[数据归档] 执行完成,结果={}", result);
        return result;
    }

    @Override
    public Map<String, Long> previewArchive(Integer days) {
        int actualDays = resolveDays(days);
        Date cutoff = Date.from(Instant.now().minus(actualDays, ChronoUnit.DAYS));
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("stocktake", archiveMapper.countStocktakeToArchive(cutoff));
        result.put("stocktakeItem", archiveMapper.countStocktakeItemToArchive(cutoff));
        result.put("purchaseOrder", archiveMapper.countPurchaseOrderToArchive(cutoff));
        result.put("purchaseOrderItem", archiveMapper.countPurchaseOrderItemToArchive(cutoff));
        result.put("sales", archiveMapper.countSalesToArchive(cutoff));
        result.put("retail", archiveMapper.countRetailToArchive(cutoff));
        result.put("inport", archiveMapper.countInportToArchive(cutoff));
        result.put("outport", archiveMapper.countOutportToArchive(cutoff));
        return result;
    }

    // ==================== 各表归档实现(每张表独立事务) ====================

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public long archiveStocktakeTable(Date cutoff) {
        Date archivedAt = new Date();
        long total = 0;
        // 1. 主表分页 INSERT 到归档表
        while (true) {
            long n = archiveMapper.archiveStocktakePage(cutoff, archivedAt, PAGE_SIZE);
            if (n == 0) break;
            total += n;
        }
        // 2. 明细分页 INSERT(基于归档主表 id 反查)
        while (true) {
            long m = archiveMapper.archiveStocktakeItemPage(cutoff, archivedAt, PAGE_SIZE);
            if (m == 0) break;
        }
        // 3. 明细分页 DELETE 原表
        while (true) {
            long m = archiveMapper.deleteArchivedStocktakeItemPage(cutoff, archivedAt, PAGE_SIZE);
            if (m == 0) break;
        }
        // 4. 主表分页 DELETE 原表
        while (true) {
            long n = archiveMapper.deleteArchivedStocktakePage(cutoff, archivedAt, PAGE_SIZE);
            if (n == 0) break;
        }
        return total;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public long archivePurchaseOrderTable(Date cutoff) {
        Date archivedAt = new Date();
        long total = 0;
        while (true) {
            long n = archiveMapper.archivePurchaseOrderPage(cutoff, archivedAt, PAGE_SIZE);
            if (n == 0) break;
            total += n;
        }
        while (true) {
            long m = archiveMapper.archivePurchaseOrderItemPage(cutoff, archivedAt, PAGE_SIZE);
            if (m == 0) break;
        }
        while (true) {
            long m = archiveMapper.deleteArchivedPurchaseOrderItemPage(cutoff, archivedAt, PAGE_SIZE);
            if (m == 0) break;
        }
        while (true) {
            long n = archiveMapper.deleteArchivedPurchaseOrderPage(cutoff, archivedAt, PAGE_SIZE);
            if (n == 0) break;
        }
        return total;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public long archiveSalesTable(Date cutoff) {
        return archiveSingleTable(
                cutoff,
                archiveMapper::archiveSalesPage,
                archiveMapper::deleteArchivedSalesPage
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public long archiveRetailTable(Date cutoff) {
        return archiveSingleTable(
                cutoff,
                archiveMapper::archiveRetailPage,
                archiveMapper::deleteArchivedRetailPage
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public long archiveInportTable(Date cutoff) {
        return archiveSingleTable(
                cutoff,
                archiveMapper::archiveInportPage,
                archiveMapper::deleteArchivedInportPage
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public long archiveOutportTable(Date cutoff) {
        return archiveSingleTable(
                cutoff,
                archiveMapper::archiveOutportPage,
                archiveMapper::deleteArchivedOutportPage
        );
    }

    // ==================== 工具方法 ====================

    /**
     * 无明细子表的单表归档(销售/零售/入库/出库)
     * 主表分页 INSERT 到归档表 → 主表分页 DELETE 原表
     */
    private long archiveSingleTable(Date cutoff,
                                    IArchiver inserter,
                                    IDeleter deleter) {
        Date archivedAt = new Date();
        long total = 0;
        // 1. 分页 INSERT 到归档表
        while (true) {
            long n = inserter.archive(cutoff, archivedAt, PAGE_SIZE);
            if (n == 0) break;
            total += n;
        }
        // 2. 分页 DELETE 原表
        while (true) {
            long n = deleter.delete(cutoff, archivedAt, PAGE_SIZE);
            if (n == 0) break;
        }
        return total;
    }

    @FunctionalInterface
    private interface IArchiver {
        long archive(Date cutoff, Date archivedAt, int limit);
    }

    @FunctionalInterface
    private interface IDeleter {
        long delete(Date cutoff, Date archivedAt, int limit);
    }

    /**
     * 安全执行单表归档:捕获异常,失败返回 -1,不影响其他表
     */
    private long safeArchive(String tableName, java.util.function.LongSupplier supplier) {
        try {
            return supplier.getAsLong();
        } catch (Exception e) {
            log.error("[数据归档] 表 {} 归档失败: {}", tableName, e.getMessage(), e);
            return -1L;
        }
    }

    private int resolveDays(Integer days) {
        return (days == null || days <= 0) ? retentionDays : days;
    }
}
