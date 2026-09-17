package com.sunlee.bus.mapper.archive;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * 业务数据归档 Mapper
 * <p>
 * 全部使用自定义 SQL,不继承 BaseMapper,绕过 MyBatis-Plus 的 @TableLogic 自动过滤,
 * 可以同时归档正常与逻辑删除的记录。
 */
@Mapper
public interface ArchiveMapper {

    // ============ 盘点单主表 ============

    /** 统计待归档的盘点单数量 */
    long countStocktakeToArchive(@Param("cutoff") Date cutoff);

    /** 分页迁移盘点单到归档表,返回实际插入条数 */
    long archiveStocktakePage(@Param("cutoff") Date cutoff,
                              @Param("archivedAt") Date archivedAt,
                              @Param("limit") int limit);

    /** 删除原表中已归档到本次 archivedAt 的盘点单,返回删除条数 */
    long deleteArchivedStocktakePage(@Param("cutoff") Date cutoff,
                                     @Param("archivedAt") Date archivedAt,
                                     @Param("limit") int limit);

    // ============ 盘点明细 ============

    long countStocktakeItemToArchive(@Param("cutoff") Date cutoff);

    long archiveStocktakeItemPage(@Param("cutoff") Date cutoff,
                                  @Param("archivedAt") Date archivedAt,
                                  @Param("limit") int limit);

    long deleteArchivedStocktakeItemPage(@Param("cutoff") Date cutoff,
                                         @Param("archivedAt") Date archivedAt,
                                         @Param("limit") int limit);

    // ============ 采购单主表 ============

    long countPurchaseOrderToArchive(@Param("cutoff") Date cutoff);

    long archivePurchaseOrderPage(@Param("cutoff") Date cutoff,
                                  @Param("archivedAt") Date archivedAt,
                                  @Param("limit") int limit);

    long deleteArchivedPurchaseOrderPage(@Param("cutoff") Date cutoff,
                                         @Param("archivedAt") Date archivedAt,
                                         @Param("limit") int limit);

    // ============ 采购单明细 ============

    long countPurchaseOrderItemToArchive(@Param("cutoff") Date cutoff);

    long archivePurchaseOrderItemPage(@Param("cutoff") Date cutoff,
                                      @Param("archivedAt") Date archivedAt,
                                      @Param("limit") int limit);

    long deleteArchivedPurchaseOrderItemPage(@Param("cutoff") Date cutoff,
                                             @Param("archivedAt") Date archivedAt,
                                             @Param("limit") int limit);

    // ============ 销售单 ============

    long countSalesToArchive(@Param("cutoff") Date cutoff);

    long archiveSalesPage(@Param("cutoff") Date cutoff,
                          @Param("archivedAt") Date archivedAt,
                          @Param("limit") int limit);

    long deleteArchivedSalesPage(@Param("cutoff") Date cutoff,
                                 @Param("archivedAt") Date archivedAt,
                                 @Param("limit") int limit);

    // ============ 零售单 ============

    long countRetailToArchive(@Param("cutoff") Date cutoff);

    long archiveRetailPage(@Param("cutoff") Date cutoff,
                           @Param("archivedAt") Date archivedAt,
                           @Param("limit") int limit);

    long deleteArchivedRetailPage(@Param("cutoff") Date cutoff,
                                  @Param("archivedAt") Date archivedAt,
                                  @Param("limit") int limit);

    // ============ 入库单 ============

    long countInportToArchive(@Param("cutoff") Date cutoff);

    long archiveInportPage(@Param("cutoff") Date cutoff,
                           @Param("archivedAt") Date archivedAt,
                           @Param("limit") int limit);

    long deleteArchivedInportPage(@Param("cutoff") Date cutoff,
                                  @Param("archivedAt") Date archivedAt,
                                  @Param("limit") int limit);

    // ============ 出库单 ============

    long countOutportToArchive(@Param("cutoff") Date cutoff);

    long archiveOutportPage(@Param("cutoff") Date cutoff,
                            @Param("archivedAt") Date archivedAt,
                            @Param("limit") int limit);

    long deleteArchivedOutportPage(@Param("cutoff") Date cutoff,
                                   @Param("archivedAt") Date archivedAt,
                                   @Param("limit") int limit);
}
