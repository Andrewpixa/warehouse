package com.sunlee.bus.service;

import java.util.Date;
import java.util.Map;

/**
 * 业务数据归档 Service
 */
public interface IDataArchiveService {

    /**
     * 执行全量归档(6 张主表 + 2 张明细表)
     *
     * @param days 保留天数,null 或 <= 0 时使用配置 archive.retention-days
     * @return key=表名,value=归档条数(主表)
     */
    Map<String, Long> runArchive(Integer days);

    /**
     * 预览每张表待归档数量(不执行迁移)
     *
     * @param days 保留天数,null 或 <= 0 时使用配置 archive.retention-days
     * @return key=表名,value=待归档条数
     */
    Map<String, Long> previewArchive(Integer days);

    /**
     * 归档盘点单(主+明细) - 独立事务
     */
    long archiveStocktakeTable(Date cutoff);

    /**
     * 归档采购单(主+明细) - 独立事务
     */
    long archivePurchaseOrderTable(Date cutoff);

    /**
     * 归档销售单 - 独立事务
     */
    long archiveSalesTable(Date cutoff);

    /**
     * 归档零售单 - 独立事务
     */
    long archiveRetailTable(Date cutoff);

    /**
     * 归档入库单 - 独立事务
     */
    long archiveInportTable(Date cutoff);

    /**
     * 归档出库单 - 独立事务
     */
    long archiveOutportTable(Date cutoff);
}
