package com.sunlee.bus.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sunlee.bus.entity.*;
import com.sunlee.bus.service.*;
import com.sunlee.bus.vo.GoodsVo;
import com.sunlee.sys.annotation.OperationLog;
import com.sunlee.sys.common.AppFileUtils;
import com.sunlee.sys.common.Constast;
import com.sunlee.sys.common.DataGridView;
import com.sunlee.sys.common.PinyinUtils;
import com.sunlee.sys.common.ResultObj;
import com.sunlee.sys.common.WebUtils;
import com.sunlee.sys.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * InnoDB free: 9216 kB; (`providerid`) REFER `warehouse/bus_provider`(`id`) 前端控制器
 * </p>
 *
 * @author sunlee
 * @since 2026-03-20
 */
@Slf4j
@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private IGoodsService goodsService;

    @Autowired
    private IProviderService providerService;

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private IInportService inportService;

    @Autowired
    private ISalesService salesService;

    @Autowired
    private IOutportService outportService;

    @Autowired
    private ISalesbackService salesbackService;

    @Autowired
    private IGoodsStockService goodsStockService;

    @Autowired
    private IDrugService drugService;

    @Autowired
    private IPurchaseOrderService purchaseOrderService;

    @Autowired
    private ISalesOrderService pharmaSalesOrderService;

    @Autowired
    private IBatchStockService batchStockService;

    /**
     * 查询商品（支持名称、拼音、简写搜索，批量查询避免 N+1）
     */
    @RequestMapping("loadAllGoods")
    public DataGridView loadAllGoods(GoodsVo goodsVo){
        try {
        IPage<Goods> page = new Page<>(goodsVo.getPage(), goodsVo.getLimit());
        QueryWrapper<Goods> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(goodsVo.getProviderid()!=null && goodsVo.getProviderid()!=0, "providerid", goodsVo.getProviderid());
        queryWrapper.eq(goodsVo.getCategoryid()!=null && goodsVo.getCategoryid()!=0, "categoryid", goodsVo.getCategoryid());
        // 支持商品名称、拼音、简写模糊搜索
        if (StringUtils.isNotBlank(goodsVo.getGoodsname())) {
            String keyword = goodsVo.getGoodsname().trim();
            queryWrapper.and(w -> w
                .like("goodsname", keyword)
                .or().like("pinyin", keyword)
                .or().like("abbreviation", keyword)
            );
        }
        queryWrapper.like(StringUtils.isNotBlank(goodsVo.getProductcode()), "productcode", goodsVo.getProductcode());
        queryWrapper.like(StringUtils.isNotBlank(goodsVo.getPromitcode()), "promitcode", goodsVo.getPromitcode());
        queryWrapper.like(StringUtils.isNotBlank(goodsVo.getDescription()), "description", goodsVo.getDescription());
        queryWrapper.like(StringUtils.isNotBlank(goodsVo.getSize()), "size", goodsVo.getSize());
        queryWrapper.like(StringUtils.isNotBlank(goodsVo.getAttribute()), "attribute", goodsVo.getAttribute());
        queryWrapper.orderByDesc("id");
        goodsService.page(page, queryWrapper);

        // 批量查询供应商和分类，避免 N+1
        List<Goods> records = page.getRecords();
        Set<Integer> providerIds = records.stream().map(Goods::getProviderid).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> categoryIds = records.stream().map(Goods::getCategoryid).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Integer, String> providerMap = new HashMap<>();
        if (!providerIds.isEmpty()) {
            providerService.listByIds(providerIds).forEach(p -> providerMap.put(p.getId(), p.getProvidername()));
        }
        Map<Integer, String> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryService.listByIds(categoryIds).forEach(c -> categoryMap.put(c.getId(), c.getCatename()));
        }

        for (Goods goods : records) {
            goods.setProvidername(providerMap.get(goods.getProviderid()));
            goods.setCategoryname(categoryMap.get(goods.getCategoryid()));
        }

        // 指定仓库时，库存列覆盖为该仓分仓库存（该仓无记录的商品按0计）
        if (goodsVo.getWarehouseId() != null && !records.isEmpty()) {
            List<Integer> goodsIds = records.stream().map(Goods::getId).collect(Collectors.toList());
            QueryWrapper<GoodsStock> stockQuery = new QueryWrapper<>();
            stockQuery.eq("warehouse_id", goodsVo.getWarehouseId());
            stockQuery.in("goodsid", goodsIds);
            Map<Integer, Integer> stockMap = goodsStockService.list(stockQuery).stream()
                .collect(Collectors.toMap(GoodsStock::getGoodsid,
                    s -> s.getNumber() != null ? s.getNumber() : 0));
            for (Goods goods : records) {
                goods.setNumber(stockMap.getOrDefault(goods.getId(), 0));
            }
        }
        return new DataGridView(page.getTotal(), records);
        } catch (Exception e) {
            log.warn("旧商品表未接入: {}", e.getMessage());
            return new DataGridView(0L, Collections.emptyList());
        }
    }

    /**
     * 添加商品
     */
    @OperationLog(type = "添加", module = "商品管理", description = "#args[0].goodsname != null ? '添加商品: ' + #args[0].goodsname : '添加商品'")
    @RequestMapping("addGoods")
    public ResultObj addGoods(GoodsVo goodsVo){
        try {
            // 手动数据校验
            if (goodsVo.getGoodsname() == null || goodsVo.getGoodsname().trim().isEmpty()) {
                return ResultObj.error("商品名称不能为空");
            }
            if (goodsVo.getNumber() != null && goodsVo.getNumber() < 0) {
                return ResultObj.error("商品数量不能为负数");
            }
            if (goodsVo.getGoodsimg()!=null && goodsVo.getGoodsimg().endsWith("_temp")){
                String newName = AppFileUtils.renameFile(goodsVo.getGoodsimg());
                goodsVo.setGoodsimg(newName);
            }
            goodsVo.setNumber(0);
            goodsVo.setAvailable(Constast.AVAILABLE_TRUE);
            // 自动生成拼音和简写
            if (StringUtils.isNotBlank(goodsVo.getGoodsname())) {
                goodsVo.setPinyin(PinyinUtils.getPingYin(goodsVo.getGoodsname()));
                goodsVo.setAbbreviation(PinyinUtils.getAbbreviation(goodsVo.getGoodsname()));
            }
            goodsService.save(goodsVo);
            return ResultObj.ADD_SUCCESS;
        } catch (Exception e) {
            log.error("添加商品失败: {}", e.getMessage(), e);
            return ResultObj.error("添加失败: " + e.getMessage());
        }
    }

    /**
     * 修改商品
     */
    @OperationLog(type = "修改", module = "商品管理", description = "#args[0].goodsname != null ? '修改商品: ' + #args[0].goodsname : '修改商品'")
    @RequestMapping("updateGoods")
    public ResultObj updateGoods(GoodsVo goodsVo){
        try {
            if (goodsVo.getGoodsimg()!=null
                    && !goodsVo.getGoodsimg().equals(Constast.DEFAULT_IMG_GOODS)
                    && goodsVo.getGoodsimg().endsWith("_temp")){
                String newName = AppFileUtils.renameFile(goodsVo.getGoodsimg());
                goodsVo.setGoodsimg(newName);
                Goods oldGoodsForImg = goodsService.getById(goodsVo.getId());
                if (oldGoodsForImg != null) {
                    String oldPath = oldGoodsForImg.getGoodsimg();
                    AppFileUtils.removeFileByPath(oldPath);
                }
            }
            Goods oldGoods = goodsService.getById(goodsVo.getId());
            if (oldGoods != null) {
                goodsVo.setNumber(oldGoods.getNumber());
            }
            // 更新拼音和简写
            if (StringUtils.isNotBlank(goodsVo.getGoodsname())) {
                goodsVo.setPinyin(PinyinUtils.getPingYin(goodsVo.getGoodsname()));
                goodsVo.setAbbreviation(PinyinUtils.getAbbreviation(goodsVo.getGoodsname()));
            }
            goodsService.updateById(goodsVo);
            return ResultObj.UPDATE_SUCCESS;
        } catch (Exception e) {
            log.error("修改商品失败: {}", e.getMessage(), e);
            return ResultObj.error("修改失败: " + e.getMessage());
        }
    }

    /**
     * 商品上下架
     */
    @OperationLog(type = "#args[1] == 1 ? '上架' : '下架'", module = "商品管理", description = "#args[1] == 1 ? '上架商品ID: ' + #args[0] : '下架商品ID: ' + #args[0]")
    @RequestMapping("updateGoodsAvailable")
    public ResultObj updateGoodsAvailable(Integer id, Integer available){
        try {
            Goods goods = new Goods();
            goods.setId(id);
            goods.setAvailable(available);
            goodsService.updateById(goods);
            return ResultObj.UPDATE_SUCCESS;
        } catch (Exception e) {
            log.error("商品上下架失败: {}", e.getMessage(), e);
            return ResultObj.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 删除商品
     */
    @OperationLog(type = "删除", module = "商品管理", description = "'删除商品ID: ' + #args[0]")
    @RequestMapping("deleteGoods")
    public ResultObj deleteGoods(Integer id, String goodsimg){
        try {
            AppFileUtils.removeFileByPath(goodsimg);
            goodsService.deleteGoodsById(id);
            return ResultObj.DELETE_SUCCESS;
        } catch (Exception e) {
            log.error("删除商品失败: {}", e.getMessage(), e);
            return ResultObj.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 加载所有可用的商品
     * @return
     */
    @RequestMapping("loadAllGoodsForSelect")
    public DataGridView loadAllGoodsForSelect(){
        try {
            QueryWrapper<Goods> queryWrapper = new QueryWrapper<Goods>();
            queryWrapper.eq("available",Constast.AVAILABLE_TRUE);
            List<Goods> list = goodsService.list(queryWrapper);
            for (Goods goods : list) {
                Provider provider = providerService.getById(goods.getProviderid());
                if (null!=provider){
                    goods.setProvidername(provider.getProvidername());
                }
            }
            return new DataGridView(list);
        } catch (Exception e) {
            log.warn("旧商品表未接入: {}", e.getMessage());
            return new DataGridView(Collections.emptyList());
        }
    }

    /**
     * POS页面加载商品（按销售量排序，支持分页）
     */
    @RequestMapping("loadGoodsForPOS")
    public DataGridView loadGoodsForPOS(Integer page, Integer limit, String keyword, Integer warehouseId){
        try {
        // 查询所有有效商品
        QueryWrapper<Goods> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("available", Constast.AVAILABLE_TRUE);
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            queryWrapper.and(w -> w
                .like("goodsname", kw)
                .or().like("pinyin", kw)
                .or().like("abbreviation", kw)
            );
        }

        List<Goods> allGoods = goodsService.list(queryWrapper);

        // 查询每个商品的销售量
        Map<Integer, Integer> salesCountMap = salesService.getSalesCountByGoodsId();

        // 设置销售量和供应商名称
        for (Goods goods : allGoods) {
            goods.setSalesCount(salesCountMap.getOrDefault(goods.getId(), 0));
            Provider provider = providerService.getById(goods.getProviderid());
            if (provider != null) {
                goods.setProvidername(provider.getProvidername());
            }
        }

        // 指定仓库时，库存数覆盖为该仓分仓库存（POS 按仓开单，库存上限/置灰均按该仓）
        if (warehouseId != null) {
            for (Goods goods : allGoods) {
                goods.setNumber(goodsStockService.getStockNumber(goods.getId(), warehouseId));
            }
        }

        // 按销售量降序排序
        allGoods.sort((a, b) -> Integer.compare(b.getSalesCount(), a.getSalesCount()));

        // 分页处理
        int pageNum = (page != null && page > 0) ? page : 1;
        int pageSize = (limit != null && limit > 0) ? limit : 30;
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allGoods.size());

        List<Goods> pageData = fromIndex < allGoods.size()
                ? allGoods.subList(fromIndex, toIndex)
                : new ArrayList<>();

        return new DataGridView((long) allGoods.size(), pageData);
        } catch (Exception e) {
            log.warn("旧商品表未接入: {}", e.getMessage());
            return new DataGridView(0L, Collections.emptyList());
        }
    }

    /**
     * 根据供应商ID查询商品信息
     * @param providerid    供应商ID
     * @return
     */
    @RequestMapping("loadGoodsByProviderId")
    public DataGridView loadGoodsByProviderId(Integer providerid,Integer allStatus,Integer warehouseId){
        try {
        QueryWrapper<Goods> queryWrapper = new QueryWrapper<Goods>();
        if (!Constast.AVAILABLE_TRUE.equals(allStatus)){
            queryWrapper.eq("available",Constast.AVAILABLE_TRUE);
        }
        queryWrapper.eq(providerid!=null,"providerid",providerid);
        List<Goods> list = goodsService.list(queryWrapper);
        for (Goods goods : list) {
            Provider provider = providerService.getById(goods.getProviderid());
            if (null!=provider){
                goods.setProvidername(provider.getProvidername());
            }
        }
        // 指定仓库时，库存数覆盖为该仓分仓库存（进货页按仓展示）
        if (warehouseId != null) {
            for (Goods goods : list) {
                goods.setNumber(goodsStockService.getStockNumber(goods.getId(), warehouseId));
            }
        }
        return new DataGridView(list);
        } catch (Exception e) {
            log.warn("旧商品表未接入: {}", e.getMessage());
            return new DataGridView(Collections.emptyList());
        }
    }

    @RequestMapping("loadAllWarningGoods")
    public DataGridView loadAllWarningGoods(){
        try {
            List<Map<String, Object>> warnings = new ArrayList<>();
            LocalDate nearExpireLine = LocalDate.now().plusDays(180);
            BigDecimal lowQtyLine = new BigDecimal("20");
            List<BatchStock> stocks = batchStockService.list();
            for (BatchStock stock : stocks) {
                boolean lowQty = stock.getQty() != null && stock.getQty().compareTo(lowQtyLine) <= 0;
                boolean nearExpire = stock.getExpireDate() != null && !stock.getExpireDate().isAfter(nearExpireLine);
                if (!lowQty && !nearExpire) {
                    continue;
                }
                Drug drug = drugService.getById(stock.getDrugId());
                String drugName = drug != null ? drug.getGenericName() : "未知药品";
                String reason = lowQty && nearExpire ? "低库存+近效期" : (lowQty ? "低库存" : "近效期");
                Map<String, Object> row = new HashMap<>();
                row.put("id", stock.getId());
                row.put("goodsname", drugName + " / " + stock.getBatchNo());
                row.put("number", stock.getQty());
                row.put("dangernum", reason);
                row.put("expireDate", stock.getExpireDate());
                row.put("qualityStatus", stock.getQualityStatus());
                warnings.add(row);
            }
            return new DataGridView((long) warnings.size(), warnings);
        } catch (Exception e) {
            log.warn("库存预警暂不可用: {}", e.getMessage());
            return new DataGridView(0L, Collections.emptyList());
        }
    }

    /**
     * 加载首页统计数据
     */
    @RequestMapping("loadDashboardStats")
    public Map<String, Object> loadDashboardStats(){
        Map<String, Object> result = new HashMap<>();
        result.put("goodsTotal", 0);
        result.put("todayInport", 0);
        result.put("todaySales", 0);
        try {
            result.put("goodsTotal", drugService.count());
        } catch (Exception e) {
            log.warn("药品总数统计失败: {}", e.getMessage());
        }
        try {
            QueryWrapper<PurchaseOrder> purchaseQuery = new QueryWrapper<>();
            purchaseQuery.eq("biz_date", java.sql.Date.valueOf(LocalDate.now()));
            purchaseQuery.eq("status", "已确认");
            result.put("todayInport", purchaseOrderService.count(purchaseQuery));
        } catch (Exception e) {
            log.warn("今日入库统计暂不可用: {}", e.getMessage());
        }
        try {
            QueryWrapper<SalesOrder> outboundQuery = new QueryWrapper<>();
            outboundQuery.eq("biz_date", java.sql.Date.valueOf(LocalDate.now()));
            outboundQuery.eq("status", "已确认");
            result.put("todaySales", pharmaSalesOrderService.count(outboundQuery));
        } catch (Exception e) {
            log.warn("今日销售统计暂不可用: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 加载最近操作记录（进货、销售、进货退货、销售退货）
     */
    @RequestMapping("loadRecentOperations")
    public DataGridView loadRecentOperations(){
        List<Map<String, Object>> allOps = new ArrayList<>();
        try {
            QueryWrapper<PurchaseOrder> purchaseQW = new QueryWrapper<>();
            purchaseQW.orderByDesc("created_at");
            purchaseQW.last("LIMIT 10");
            for (PurchaseOrder item : purchaseOrderService.list(purchaseQW)) {
                Map<String, Object> op = new HashMap<>();
                op.put("type", "采购入库");
                op.put("typeTag", "success");
                op.put("goodsname", item.getOrderNo());
                op.put("number", item.getTotalAmount());
                op.put("time", item.getConfirmedAt() != null ? item.getConfirmedAt() : item.getCreatedAt());
                op.put("operateperson", item.getStatus());
                allOps.add(op);
            }

            QueryWrapper<SalesOrder> outboundQW = new QueryWrapper<>();
            outboundQW.orderByDesc("created_at");
            outboundQW.last("LIMIT 10");
            for (SalesOrder item : pharmaSalesOrderService.list(outboundQW)) {
                Map<String, Object> op = new HashMap<>();
                op.put("type", "销售出库");
                op.put("typeTag", "primary");
                op.put("goodsname", item.getInvoiceNo() != null ? item.getInvoiceNo() : item.getOrderNo());
                op.put("number", item.getTotalAmount());
                op.put("time", item.getConfirmedAt() != null ? item.getConfirmedAt() : item.getCreatedAt());
                op.put("operateperson", item.getStatus());
                allOps.add(op);
            }

            allOps.sort((a, b) -> {
                Date timeA = (Date) a.get("time");
                Date timeB = (Date) b.get("time");
                if (timeA == null && timeB == null) return 0;
                if (timeA == null) return 1;
                if (timeB == null) return -1;
                return timeB.compareTo(timeA);
            });
            if (allOps.size() > 10) {
                allOps = allOps.subList(0, 10);
            }
            return new DataGridView((long) allOps.size(), allOps);
        } catch (Exception e) {
            log.warn("最近出入库记录暂不可用: {}", e.getMessage());
            return new DataGridView(0L, Collections.emptyList());
        }
    }

    /**
     * 批量生成已有商品的拼音和简写（数据迁移用）
     */
    @RequestMapping("regeneratePinyin")
    public ResultObj regeneratePinyin() {
        try {
            List<Goods> allGoods = goodsService.list();
            int count = 0;
            for (Goods goods : allGoods) {
                if (StringUtils.isNotBlank(goods.getGoodsname())) {
                    goods.setPinyin(PinyinUtils.getPingYin(goods.getGoodsname()));
                    goods.setAbbreviation(PinyinUtils.getAbbreviation(goods.getGoodsname()));
                    goodsService.updateById(goods);
                    count++;
                }
            }
            return new ResultObj(200, "成功更新 " + count + " 个商品的拼音");
        } catch (Exception e) {
            log.error("批量生成拼音失败: {}", e.getMessage(), e);
            return new ResultObj(-1, "生成拼音失败");
        }
    }

}

