package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.Customer;
import com.sunlee.bus.entity.Drug;
import com.sunlee.bus.entity.PurchaseOrder;
import com.sunlee.bus.entity.PurchaseOrderItem;
import com.sunlee.bus.entity.SalesOrder;
import com.sunlee.bus.entity.SalesOrderItem;
import com.sunlee.bus.entity.Supplier;
import com.sunlee.bus.entity.TraceCode;
import com.sunlee.bus.entity.TracePackRelation;
import com.sunlee.bus.mapper.TraceCodeMapper;
import com.sunlee.bus.service.ICustomerService;
import com.sunlee.bus.service.IDrugService;
import com.sunlee.bus.service.IPurchaseOrderItemService;
import com.sunlee.bus.service.IPurchaseOrderService;
import com.sunlee.bus.service.ISalesOrderItemService;
import com.sunlee.bus.service.ISalesOrderService;
import com.sunlee.bus.service.ISupplierService;
import com.sunlee.bus.service.ITraceCodeService;
import com.sunlee.bus.service.ITracePackRelationService;
import com.sunlee.bus.vo.TraceLookupCandidate;
import com.sunlee.bus.vo.TraceLookupResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sunlee.bus.vo.TracePackExplainVo;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class TraceCodeServiceImpl extends ServiceImpl<TraceCodeMapper, TraceCode> implements ITraceCodeService {

    @Autowired
    @Lazy
    private ISalesOrderService salesOrderService;

    @Autowired
    private ISalesOrderItemService salesOrderItemService;

    @Autowired
    @Lazy
    private IPurchaseOrderService purchaseOrderService;

    @Autowired
    private IPurchaseOrderItemService purchaseOrderItemService;

    @Autowired
    private ISupplierService supplierService;

    @Autowired
    private ICustomerService customerService;

    @Autowired
    private IDrugService drugService;

    @Autowired
    private ITracePackRelationService tracePackRelationService;

    @Override
    public TraceLookupResult lookup(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            throw new IllegalArgumentException("请输入追溯码、SPDID、采购入库单号、供应商发票、批号、出库单号或销售发票号");
        }
        String q = keyword.trim();
        for (String v : keywordVariants(q)) {
            TraceLookupResult hit = lookupOnce(v);
            if (hit != null) {
                hit.setKeyword(q);
                return hit;
            }
        }
        throw new IllegalArgumentException("未找到：" + q + "。可查追溯码、商品条码、SPDID、采购入库单号、供应商发票、批号、出库单号或销售发票号");
    }

    private List<String> keywordVariants(String q) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add(q);
        String compact = q.replaceAll("[\\s()（）-]", "");
        if (!compact.equals(q)) {
            set.add(compact);
        }
        if (compact.startsWith("01") && compact.length() > 14) {
            set.add(compact.substring(2));
        }
        String digits = compact.replaceAll("[^0-9]", "");
        if (StringUtils.isNotBlank(digits)) {
            set.add(digits);
            if (digits.startsWith("0") && digits.length() > 8) {
                set.add(digits.replaceFirst("^0+", ""));
            }
            if (!digits.startsWith("0") && digits.length() == 13) {
                set.add("0" + digits);
            }
        }
        return new ArrayList<>(set);
    }

    private TraceLookupResult lookupOnce(String q) {
        TraceLookupResult byCode = lookupByTraceCode(q);
        if (byCode != null) {
            return byCode;
        }
        TraceLookupResult byBarcode = lookupByDrugBarcode(q);
        if (byBarcode != null) {
            return byBarcode;
        }
        TraceLookupResult bySpdid = lookupBySpdid(q);
        if (bySpdid != null) {
            return bySpdid;
        }
        PurchaseOrder purchaseHit = purchaseOrderService.findByQuery(q);
        if (purchaseHit != null) {
            boolean orderExact = q.equals(purchaseHit.getOrderNo());
            return mergeInOut(q, orderExact ? "PURCHASE_ORDER" : "PURCHASE_INVOICE",
                    orderExact ? "采购入库单号" : "供应商发票号", purchaseHit.getId(), null);
        }
        SalesOrder salesHit = salesOrderService.findByInvoiceQuery(q);
        if (salesHit != null) {
            boolean orderExact = q.equals(salesHit.getOrderNo());
            return mergeInOut(q, orderExact ? "SALES_ORDER" : "SALES_INVOICE",
                    orderExact ? "出库单号" : "销售发票号", null, salesHit.getId());
        }
        return lookupByBatch(q);
    }

    private TraceLookupResult lookupByDrugBarcode(String barcode) {
        QueryWrapper<Drug> dqw = new QueryWrapper<>();
        dqw.eq("barcode", barcode);
        dqw.last("LIMIT 1");
        Drug drug = drugService.getOne(dqw, false);
        if (drug == null || drug.getId() == null) {
            return null;
        }
        QueryWrapper<PurchaseOrderItem> pqw = new QueryWrapper<>();
        pqw.eq("drug_id", drug.getId());
        pqw.orderByDesc("id");
        pqw.last("LIMIT 1");
        PurchaseOrderItem pItem = purchaseOrderItemService.getOne(pqw, false);
        QueryWrapper<SalesOrderItem> sqw = new QueryWrapper<>();
        sqw.eq("drug_id", drug.getId());
        sqw.orderByDesc("id");
        sqw.last("LIMIT 1");
        SalesOrderItem sItem = salesOrderItemService.getOne(sqw, false);
        if (pItem == null && sItem == null) {
            return null;
        }
        String batch = pItem != null ? pItem.getBatchNo() : sItem.getBatchNo();
        if (StringUtils.isNotBlank(batch)) {
            TraceLookupResult byBatch = lookupByBatch(batch);
            if (byBatch != null) {
                byBatch.setMatchType("BARCODE");
                byBatch.setMatchTypeLabel("商品条码");
                byBatch.setKeyword(barcode);
                return byBatch;
            }
        }
        return mergeInOut(barcode, "BARCODE", "商品条码",
                pItem != null ? pItem.getOrderId() : null,
                sItem != null ? sItem.getOrderId() : null);
    }

    private TraceLookupResult lookupByTraceCode(String code) {
        QueryWrapper<TraceCode> qw = new QueryWrapper<>();
        qw.eq("code", code);
        qw.last("LIMIT 1");
        TraceCode row = this.getOne(qw, false);
        if (row == null) {
            return null;
        }
        TraceLookupResult bySpdid = lookupBySpdid(row.getSpdid());
        if (bySpdid != null) {
            bySpdid.setKeyword(code);
            bySpdid.setMatchType("TRACE_CODE");
            bySpdid.setMatchTypeLabel("追溯码");
            return bySpdid;
        }
        TraceLookupResult result = new TraceLookupResult();
        result.setKeyword(code);
        result.setMatchType("TRACE_CODE");
        result.setMatchTypeLabel("追溯码");
        result.setBillType(StringUtils.defaultIfBlank(row.getBizType(), ""));
        List<TraceCode> traces = listBySpdid(row.getSpdid());
        if (traces.isEmpty()) {
            traces.add(row);
        }
        result.setTraces(traces);
        decorateAll(traces);
        return result;
    }

    private TraceLookupResult lookupBySpdid(String spdid) {
        if (StringUtils.isBlank(spdid)) {
            return null;
        }
        QueryWrapper<PurchaseOrderItem> pqw = new QueryWrapper<>();
        pqw.eq("spdid", spdid);
        pqw.last("LIMIT 1");
        PurchaseOrderItem pItem = purchaseOrderItemService.getOne(pqw, false);
        QueryWrapper<SalesOrderItem> sqw = new QueryWrapper<>();
        sqw.eq("spdid", spdid);
        sqw.last("LIMIT 1");
        SalesOrderItem sItem = salesOrderItemService.getOne(sqw, false);
        Long purchaseId = pItem != null ? pItem.getOrderId() : null;
        Long salesId = sItem != null ? sItem.getOrderId() : null;
        String batch = pItem != null ? pItem.getBatchNo() : (sItem != null ? sItem.getBatchNo() : null);
        if (purchaseId == null && StringUtils.isNotBlank(batch)) {
            QueryWrapper<PurchaseOrderItem> byBatch = new QueryWrapper<>();
            byBatch.eq("batch_no", batch);
            byBatch.last("LIMIT 1");
            PurchaseOrderItem pByBatch = purchaseOrderItemService.getOne(byBatch, false);
            if (pByBatch != null) {
                purchaseId = pByBatch.getOrderId();
            }
        }
        if (salesId == null && StringUtils.isNotBlank(batch)) {
            QueryWrapper<SalesOrderItem> byBatch = new QueryWrapper<>();
            byBatch.eq("batch_no", batch);
            byBatch.last("LIMIT 1");
            SalesOrderItem sByBatch = salesOrderItemService.getOne(byBatch, false);
            if (sByBatch != null) {
                salesId = sByBatch.getOrderId();
            }
        }
        if (purchaseId == null && salesId == null) {
            return null;
        }
        return mergeInOut(spdid, "SPDID", "SPDID / 流水号", purchaseId, salesId);
    }

    private TraceLookupResult lookupByBatch(String batchNo) {
        QueryWrapper<PurchaseOrderItem> pqw = new QueryWrapper<>();
        pqw.eq("batch_no", batchNo);
        pqw.last("LIMIT 20");
        List<PurchaseOrderItem> pItems = purchaseOrderItemService.list(pqw);
        QueryWrapper<SalesOrderItem> sqw = new QueryWrapper<>();
        sqw.eq("batch_no", batchNo);
        sqw.last("LIMIT 20");
        List<SalesOrderItem> sItems = salesOrderItemService.list(sqw);
        if (pItems.isEmpty() && sItems.isEmpty()) {
            return null;
        }
        if (pItems.size() <= 1 && sItems.size() <= 1) {
            return mergeInOut(batchNo, "BATCH", "批号",
                    pItems.isEmpty() ? null : pItems.get(0).getOrderId(),
                    sItems.isEmpty() ? null : sItems.get(0).getOrderId());
        }
        TraceLookupResult result = new TraceLookupResult();
        result.setKeyword(batchNo);
        result.setMatchType("BATCH");
        result.setMatchTypeLabel("批号（多条候选）");
        List<TraceLookupCandidate> candidates = new ArrayList<>();
        for (PurchaseOrderItem item : pItems) {
            candidates.add(toPurchaseCandidate(item));
        }
        for (SalesOrderItem item : sItems) {
            candidates.add(toSalesCandidate(item));
        }
        result.setCandidates(candidates);
        return result;
    }

    private TraceLookupResult ofPurchase(String keyword, String matchType, String label, Long orderId) {
        return mergeInOut(keyword, matchType, label, orderId, null);
    }

    private TraceLookupResult ofSales(String keyword, String matchType, String label, Long orderId) {
        return mergeInOut(keyword, matchType, label, null, orderId);
    }

    private TraceLookupResult mergeInOut(String keyword, String matchType, String label, Long purchaseOrderId, Long salesOrderId) {
        TraceLookupResult result = new TraceLookupResult();
        result.setKeyword(keyword);
        result.setMatchType(matchType);
        result.setMatchTypeLabel(label);
        List<TraceCode> traces = new ArrayList<>();
        if (purchaseOrderId != null) {
            PurchaseOrder order = purchaseOrderService.getDetail(purchaseOrderId);
            result.setPurchase(order);
            if (order != null) {
                traces.addAll(collectPurchaseTraces(order));
            }
        }
        if (salesOrderId != null) {
            SalesOrder order = salesOrderService.getDetail(salesOrderId);
            if (order != null && order.getId() != null) {
                ensureLogisticsCodes(order.getId());
                order = salesOrderService.getDetail(salesOrderId);
            }
            result.setOutbound(order);
            if (order != null) {
                traces.addAll(collectSalesTraces(order));
            }
        }
        if (purchaseOrderId != null && salesOrderId != null) {
            result.setBillType("入库 + 出库");
        } else if (purchaseOrderId != null) {
            result.setBillType("入库");
        } else {
            result.setBillType("出库");
        }
        result.setTraces(traces);
        decorateAll(traces);
        return result;
    }

    private List<TraceCode> collectPurchaseTraces(PurchaseOrder order) {
        List<TraceCode> result = new ArrayList<>();
        if (order.getItems() == null) {
            return result;
        }
        for (PurchaseOrderItem item : order.getItems()) {
            if (StringUtils.isBlank(item.getSpdid())) {
                continue;
            }
            Drug drug = item.getDrugId() == null ? null : drugService.getById(item.getDrugId());
            for (TraceCode code : listBySpdid(item.getSpdid())) {
                code.setInvoiceNo(order.getInvoiceNo());
                code.setOrderNo(order.getOrderNo());
                code.setBatchNo(item.getBatchNo());
                if (drug != null) {
                    code.setDrugName(drug.getGenericName());
                }
                result.add(code);
            }
        }
        return result;
    }

    private List<TraceCode> collectSalesTraces(SalesOrder order) {
        List<TraceCode> result = new ArrayList<>();
        if (order.getItems() == null) {
            return result;
        }
        for (SalesOrderItem item : order.getItems()) {
            if (StringUtils.isBlank(item.getSpdid())) {
                continue;
            }
            Drug drug = item.getDrugId() == null ? null : drugService.getById(item.getDrugId());
            for (TraceCode code : listBySpdid(item.getSpdid())) {
                code.setInvoiceNo(order.getInvoiceNo());
                code.setOrderNo(order.getOrderNo());
                code.setBatchNo(item.getBatchNo());
                if (drug != null) {
                    code.setDrugName(drug.getGenericName());
                }
                result.add(code);
            }
        }
        return result;
    }

    private TraceLookupCandidate toPurchaseCandidate(PurchaseOrderItem item) {
        TraceLookupCandidate c = new TraceLookupCandidate();
        c.setBillType("入库");
        c.setOrderId(item.getOrderId());
        c.setSpdid(item.getSpdid());
        c.setBatchNo(item.getBatchNo());
        Drug drug = item.getDrugId() == null ? null : drugService.getById(item.getDrugId());
        if (drug != null) {
            c.setDrugName(drug.getGenericName());
        }
        PurchaseOrder order = purchaseOrderService.getById(item.getOrderId());
        if (order != null) {
            c.setOrderNo(order.getOrderNo());
            c.setInvoiceNo(order.getInvoiceNo());
            Supplier supplier = order.getSupplierId() == null ? null : supplierService.getById(order.getSupplierId());
            if (supplier != null) {
                c.setPartyName(supplier.getName());
            }
        }
        return c;
    }

    private TraceLookupCandidate toSalesCandidate(SalesOrderItem item) {
        TraceLookupCandidate c = new TraceLookupCandidate();
        c.setBillType("出库");
        c.setOrderId(item.getOrderId());
        c.setSpdid(item.getSpdid());
        c.setBatchNo(item.getBatchNo());
        Drug drug = item.getDrugId() == null ? null : drugService.getById(item.getDrugId());
        if (drug != null) {
            c.setDrugName(drug.getGenericName());
        }
        SalesOrder order = salesOrderService.getById(item.getOrderId());
        if (order != null) {
            c.setOrderNo(order.getOrderNo());
            c.setInvoiceNo(order.getInvoiceNo());
            Customer customer = order.getCustomerId() == null ? null : customerService.getById(order.getCustomerId());
            if (customer != null) {
                c.setPartyName(customer.getName());
            }
        }
        return c;
    }

    @Override
    public List<TraceCode> listByInvoiceNo(String invoiceNo) {
        if (StringUtils.isBlank(invoiceNo)) {
            return new ArrayList<>();
        }
        SalesOrder order = salesOrderService.findByInvoiceQuery(invoiceNo);
        if (order == null) {
            return new ArrayList<>();
        }
        ensureLogisticsCodes(order.getId());
        QueryWrapper<SalesOrderItem> itemQw = new QueryWrapper<>();
        itemQw.eq("order_id", order.getId());
        List<SalesOrderItem> items = salesOrderItemService.list(itemQw);
        List<TraceCode> result = new ArrayList<>();
        for (SalesOrderItem item : items) {
            if (StringUtils.isBlank(item.getSpdid())) {
                continue;
            }
            List<TraceCode> codes = listBySpdid(item.getSpdid());
            Drug drug = drugService.getById(item.getDrugId());
            for (TraceCode code : codes) {
                code.setInvoiceNo(order.getInvoiceNo());
                code.setOrderNo(order.getOrderNo());
                code.setBatchNo(item.getBatchNo());
                if (drug != null) {
                    code.setDrugName(drug.getGenericName());
                }
                result.add(code);
            }
        }
        decorateAll(result);
        return result;
    }

    @Override
    public List<TraceCode> listBySpdid(String spdid) {
        if (StringUtils.isBlank(spdid)) {
            return new ArrayList<>();
        }
        QueryWrapper<TraceCode> qw = new QueryWrapper<>();
        qw.eq("spdid", spdid);
        qw.orderByAsc("id");
        List<TraceCode> rows = this.list(qw);
        decorateAll(rows);
        return rows;
    }

    @Override
    @Transactional
    public void ensureLogisticsCodes(Long salesOrderId) {
        if (salesOrderId == null) {
            return;
        }
        SalesOrder order = salesOrderService.getById(salesOrderId);
        if (order == null) {
            return;
        }
        if (!com.sunlee.bus.common.PharmaNos.STATUS_CONFIRMED.equals(order.getStatus())) {
            return;
        }
        if (com.sunlee.bus.common.PharmaNos.ORDER_REVERSAL.equals(order.getOrderType())) {
            return;
        }
        QueryWrapper<SalesOrderItem> itemQw = new QueryWrapper<>();
        itemQw.eq("order_id", salesOrderId);
        List<SalesOrderItem> items = salesOrderItemService.list(itemQw);
        Date now = new Date();
        for (SalesOrderItem item : items) {
            if (StringUtils.isBlank(item.getSpdid())) {
                continue;
            }
            List<TraceCode> existing = this.list(new QueryWrapper<TraceCode>().eq("spdid", item.getSpdid()));
            boolean hasReal = false;
            for (TraceCode row : existing) {
                if (!"缺码".equals(row.getStatus())) {
                    hasReal = true;
                    break;
                }
            }
            if (hasReal) {
                continue;
            }
            int qty = 1;
            if (item.getQty() != null && item.getQty().intValue() > 0) {
                qty = Math.min(item.getQty().intValue(), 20);
            }
            for (int i = 1; i <= qty; i++) {
                String code = logisticsSmallCode(item.getSpdid(), i);
                if (this.count(new QueryWrapper<TraceCode>().eq("code", code)) > 0) {
                    code = logisticsSmallCode(item.getSpdid(), i + 1000);
                }
                TraceCode row = new TraceCode();
                row.setSpdid(item.getSpdid().trim());
                row.setCode(code);
                row.setPackLevel("最小包装");
                row.setBizType("出库");
                row.setStatus("正常");
                row.setCollectedAt(now);
                row.setRemark("物流匹配");
                this.save(row);
            }
        }
    }

    @Override
    @Transactional
    public int replaceAbnormal(Long traceId, String newCode, boolean useUniversal01, String remark) {
        if (traceId == null) {
            throw new IllegalArgumentException("请选择要处理的异常码");
        }
        TraceCode old = this.getById(traceId);
        if (old == null) {
            throw new IllegalArgumentException("追溯码记录不存在");
        }
        String code = useUniversal01 ? nextUniversal01() : StringUtils.trimToNull(newCode);
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("请扫描正确追溯码，或选择用 01 码代替");
        }
        if (this.count(new QueryWrapper<TraceCode>().eq("code", code)) > 0) {
            throw new IllegalArgumentException("该码已存在：" + code);
        }
        old.setStatus("异常");
        old.setRemark(StringUtils.defaultIfBlank(remark, "错码/异常，已替换"));
        this.updateById(old);

        TraceCode neu = new TraceCode();
        neu.setSpdid(old.getSpdid());
        neu.setCode(code);
        neu.setPackLevel(StringUtils.defaultIfBlank(old.getPackLevel(), "最小包装"));
        neu.setParentCode(old.getParentCode());
        neu.setBizType(StringUtils.defaultIfBlank(old.getBizType(), "出库"));
        neu.setStatus(useUniversal01 ? "01替代" : "正常");
        neu.setCollectedAt(new Date());
        neu.setRemark(useUniversal01 ? "异常码已用 01 码代替" : "异常纠码");
        this.save(neu);
        return 1;
    }

    @Override
    @Transactional
    public int reportMissing(String spdid, String customerNote) {
        if (StringUtils.isBlank(spdid)) {
            throw new IllegalArgumentException("SPDID 不能为空");
        }
        if (StringUtils.isBlank(customerNote)) {
            throw new IllegalArgumentException("缺码须填写客户问题描述");
        }
        String code = "MISS-" + spdid.trim() + "-" + System.currentTimeMillis();
        TraceCode row = new TraceCode();
        row.setSpdid(spdid.trim());
        row.setCode(code);
        row.setPackLevel("最小包装");
        row.setBizType("出库");
        row.setStatus("缺码");
        row.setCollectedAt(new Date());
        row.setRemark(customerNote.trim());
        this.save(row);
        return 1;
    }

    private void decorateAll(List<TraceCode> rows) {
        if (rows == null) {
            return;
        }
        for (TraceCode code : rows) {
            code.setCodeKind(detectKind(code));
        }
    }

    private String detectKind(TraceCode code) {
        if (code == null) {
            return "";
        }
        if ("缺码".equals(code.getStatus())) {
            return "缺码";
        }
        if ("01替代".equals(code.getStatus()) || looks01(code.getCode())) {
            return "01替代";
        }
        if (looksGs1(code.getCode())) {
            return "GS1";
        }
        return "正常药监码";
    }

    private boolean looks01(String code) {
        if (StringUtils.isBlank(code)) {
            return false;
        }
        return code.endsWith("0001") || code.contains("00000000000000000001");
    }

    private boolean looksGs1(String code) {
        if (StringUtils.isBlank(code) || code.length() <= 20) {
            return false;
        }
        return code.startsWith("00") || code.startsWith("01") || code.startsWith("11");
    }

    private String logisticsSmallCode(String spdid, int seq) {
        int h = (spdid + "#" + seq).hashCode() & 0x7fffffff;
        return "8" + String.format("%019d", (long) h);
    }

    private String nextUniversal01() {
        long n = System.currentTimeMillis() % 1_000_000_000L;
        return String.format("%016d", n) + "0001";
    }

    @Override
    @Transactional
    public int addCodes(String spdid, List<String> codes, String packLevel, String bizType) {
        return addCodes(spdid, codes, packLevel, bizType, false);
    }

    @Override
    @Transactional
    public int addCodes(String spdid, List<String> codes, String packLevel, String bizType, boolean skipDuplicate) {
        if (StringUtils.isBlank(spdid)) {
            throw new IllegalArgumentException("SPDID 不能为空");
        }
        if (codes == null || codes.isEmpty()) {
            throw new IllegalArgumentException("请至少录入一条追溯码");
        }
        Date now = new Date();
        int saved = 0;
        for (String raw : codes) {
            if (StringUtils.isBlank(raw)) {
                continue;
            }
            String code = raw.trim();
            QueryWrapper<TraceCode> existQw = new QueryWrapper<>();
            existQw.eq("code", code);
            if (this.count(existQw) > 0) {
                if (skipDuplicate) {
                    continue;
                }
                throw new IllegalArgumentException("追溯码已存在：" + code);
            }
            TraceCode row = new TraceCode();
            row.setSpdid(spdid.trim());
            row.setCode(code);
            row.setPackLevel(StringUtils.defaultIfBlank(packLevel, "最小包装"));
            row.setBizType(StringUtils.defaultIfBlank(bizType, "出库"));
            row.setStatus("正常");
            row.setCollectedAt(now);
            this.save(row);
            saved++;
        }
        if (saved == 0 && !skipDuplicate) {
            throw new IllegalArgumentException("没有可保存的追溯码");
        }
        return saved;
    }

    @Override
    public List<TraceCode> previewParse(String parentCode) {
        return expandPack(parentCode);
    }

    @Override
    @Transactional
    public int parseAndCollect(String spdid, String parentCode, String bizType) {
        if (StringUtils.isBlank(spdid)) {
            throw new IllegalArgumentException("SPDID 不能为空");
        }
        List<TraceCode> expanded = expandPack(parentCode);
        Date now = new Date();
        int saved = 0;
        for (TraceCode row : expanded) {
            QueryWrapper<TraceCode> existQw = new QueryWrapper<>();
            existQw.eq("code", row.getCode());
            TraceCode exist = this.getOne(existQw, false);
            if (exist != null) {
                if (!spdid.trim().equals(exist.getSpdid())) {
                    throw new IllegalArgumentException("追溯码已被其他明细占用：" + row.getCode());
                }
                if (StringUtils.isBlank(exist.getParentCode()) && StringUtils.isNotBlank(row.getParentCode())) {
                    exist.setParentCode(row.getParentCode());
                    exist.setPackLevel(row.getPackLevel());
                    this.updateById(exist);
                }
                continue;
            }
            row.setSpdid(spdid.trim());
            row.setBizType(StringUtils.defaultIfBlank(bizType, "出库"));
            row.setStatus("正常");
            row.setCollectedAt(now);
            this.save(row);
            saved++;
        }
        if (saved == 0) {
            throw new IllegalArgumentException("解析结果均已采集，无需重复写入");
        }
        return saved;
    }

    private List<TraceCode> expandPack(String parentCode) {
        if (StringUtils.isBlank(parentCode)) {
            throw new IllegalArgumentException("请输入要解析的包装码");
        }
        String root = parentCode.trim();
        List<TracePackRelation> children = listChildren(root);
        if (children.isEmpty()) {
            throw new IllegalArgumentException("未找到该码的下级包装关系，最小包装不可再解析");
        }
        List<TraceCode> result = new ArrayList<>();
        TraceCode rootRow = new TraceCode();
        rootRow.setCode(root);
        rootRow.setPackLevel(children.get(0).getParentLevel());
        result.add(rootRow);
        walkChildren(root, result);
        return result;
    }

    private void walkChildren(String parent, List<TraceCode> out) {
        for (TracePackRelation rel : listChildren(parent)) {
            TraceCode child = new TraceCode();
            child.setCode(rel.getChildCode());
            child.setPackLevel(rel.getChildLevel());
            child.setParentCode(parent);
            out.add(child);
            walkChildren(rel.getChildCode(), out);
        }
    }

    private List<TracePackRelation> listChildren(String parentCode) {
        QueryWrapper<TracePackRelation> qw = new QueryWrapper<>();
        qw.eq("parent_code", parentCode);
        qw.orderByAsc("id");
        return tracePackRelationService.list(qw);
    }

    @Override
    public TracePackExplainVo explainPack(String code) {
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("请输入大码");
        }
        String input = code.trim();
        String root = climbToRoot(input);
        TracePackExplainVo vo = new TracePackExplainVo();
        vo.setInputCode(input);
        vo.setRootCode(root);
        List<TraceCode> tree;
        try {
            tree = expandPack(root);
        } catch (IllegalArgumentException ex) {
            tree = new ArrayList<>();
            TraceCode self = new TraceCode();
            self.setCode(root);
            TraceCode collected = getByCode(root);
            self.setPackLevel(collected == null ? "大包装" : collected.getPackLevel());
            tree.add(self);
        }
        vo.setRootLevel(tree.isEmpty() ? "大包装" : tree.get(0).getPackLevel());
        for (TraceCode row : tree) {
            String kind = packKind(row.getPackLevel());
            if ("big".equals(kind)) {
                vo.getBigCodes().add(row.getCode());
            } else if ("mid".equals(kind)) {
                vo.getMidCodes().add(row.getCode());
            } else {
                vo.getSmallCodes().add(row.getCode());
            }
        }
        Set<String> allCodes = new LinkedHashSet<>();
        allCodes.add(input);
        allCodes.add(root);
        tree.forEach(t -> allCodes.add(t.getCode()));
        QueryWrapper<TraceCode> qw = new QueryWrapper<>();
        qw.in("code", allCodes);
        List<TraceCode> collected = this.list(qw);
        Set<String> spdids = new LinkedHashSet<>();
        for (TraceCode t : collected) {
            if (StringUtils.isNotBlank(t.getSpdid())) {
                spdids.add(t.getSpdid());
            }
        }
        fillBatchAndMoves(vo, spdids);
        return vo;
    }

    private String climbToRoot(String code) {
        String current = code;
        for (int i = 0; i < 8; i++) {
            QueryWrapper<TracePackRelation> qw = new QueryWrapper<>();
            qw.eq("child_code", current);
            qw.last("LIMIT 1");
            TracePackRelation parent = tracePackRelationService.getOne(qw, false);
            if (parent == null || StringUtils.isBlank(parent.getParentCode())) {
                break;
            }
            current = parent.getParentCode();
        }
        return current;
    }

    private TraceCode getByCode(String code) {
        QueryWrapper<TraceCode> qw = new QueryWrapper<>();
        qw.eq("code", code);
        qw.last("LIMIT 1");
        return this.getOne(qw, false);
    }

    private String packKind(String level) {
        if (level == null) {
            return "small";
        }
        if (level.contains("大")) {
            return "big";
        }
        if (level.contains("中")) {
            return "mid";
        }
        return "small";
    }

    private void fillBatchAndMoves(TracePackExplainVo vo, Set<String> spdids) {
        Set<Long> purchaseIds = new LinkedHashSet<>();
        Set<Long> salesIds = new LinkedHashSet<>();
        String batchNo = null;
        String expire = null;
        String drugName = null;
        String spec = null;
        for (String spdid : spdids) {
            QueryWrapper<PurchaseOrderItem> pqw = new QueryWrapper<>();
            pqw.eq("spdid", spdid);
            pqw.last("LIMIT 1");
            PurchaseOrderItem pItem = purchaseOrderItemService.getOne(pqw, false);
            if (pItem != null) {
                purchaseIds.add(pItem.getOrderId());
                if (batchNo == null) {
                    batchNo = pItem.getBatchNo();
                    expire = pItem.getExpireDate() == null ? null : pItem.getExpireDate().toString();
                    Drug drug = pItem.getDrugId() == null ? null : drugService.getById(pItem.getDrugId());
                    if (drug != null) {
                        drugName = drug.getGenericName();
                        spec = drug.getSpec();
                    }
                }
            }
            QueryWrapper<SalesOrderItem> sqw = new QueryWrapper<>();
            sqw.eq("spdid", spdid);
            sqw.last("LIMIT 1");
            SalesOrderItem sItem = salesOrderItemService.getOne(sqw, false);
            if (sItem != null) {
                salesIds.add(sItem.getOrderId());
                if (batchNo == null) {
                    batchNo = sItem.getBatchNo();
                    expire = sItem.getExpireDate() == null ? null : sItem.getExpireDate().toString();
                    Drug drug = sItem.getDrugId() == null ? null : drugService.getById(sItem.getDrugId());
                    if (drug != null) {
                        drugName = drug.getGenericName();
                        spec = drug.getSpec();
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(batchNo)) {
            QueryWrapper<PurchaseOrderItem> byBatch = new QueryWrapper<>();
            byBatch.eq("batch_no", batchNo);
            for (PurchaseOrderItem item : purchaseOrderItemService.list(byBatch)) {
                purchaseIds.add(item.getOrderId());
            }
            QueryWrapper<SalesOrderItem> sBatch = new QueryWrapper<>();
            sBatch.eq("batch_no", batchNo);
            for (SalesOrderItem item : salesOrderItemService.list(sBatch)) {
                salesIds.add(item.getOrderId());
            }
        }
        vo.setBatchNo(batchNo);
        vo.setExpireDate(expire);
        vo.setDrugName(drugName);
        vo.setSpec(spec);
        for (Long id : purchaseIds) {
            if (id == null) {
                continue;
            }
            PurchaseOrder order = purchaseOrderService.getDetail(id);
            if (order == null) {
                continue;
            }
            Supplier supplier = order.getSupplierId() == null ? null : supplierService.getById(order.getSupplierId());
            if (order.getItems() != null) {
                for (PurchaseOrderItem item : order.getItems()) {
                    if (batchNo != null && !batchNo.equals(item.getBatchNo())) {
                        continue;
                    }
                    TracePackExplainVo.Move m = new TracePackExplainVo.Move();
                    m.setBillType("入库");
                    m.setOrderNo(order.getOrderNo());
                    m.setInvoiceNo(order.getInvoiceNo());
                    m.setPartyName(supplier == null ? null : supplier.getName());
                    m.setWarehouseName(order.getWarehouseName());
                    m.setBizDate(order.getBizDate() == null ? null : order.getBizDate().toString());
                    m.setStatus(order.getStatus());
                    m.setSpdid(item.getSpdid());
                    m.setQty(item.getStockInQty() == null ? null : item.getStockInQty().toPlainString());
                    Drug drug = item.getDrugId() == null ? null : drugService.getById(item.getDrugId());
                    m.setDrugName(drug == null ? vo.getDrugName() : drug.getGenericName());
                    m.setBatchNo(item.getBatchNo());
                    vo.getInbounds().add(m);
                }
            }
        }
        for (Long id : salesIds) {
            if (id == null) {
                continue;
            }
            SalesOrder order = salesOrderService.getDetail(id);
            if (order == null) {
                continue;
            }
            Customer customer = order.getCustomerId() == null ? null : customerService.getById(order.getCustomerId());
            if (order.getItems() != null) {
                for (SalesOrderItem item : order.getItems()) {
                    if (batchNo != null && !batchNo.equals(item.getBatchNo())) {
                        continue;
                    }
                    TracePackExplainVo.Move m = new TracePackExplainVo.Move();
                    m.setBillType("出库");
                    m.setOrderNo(order.getOrderNo());
                    m.setInvoiceNo(order.getInvoiceNo());
                    m.setPartyName(customer == null ? null : customer.getName());
                    m.setWarehouseName(order.getWarehouseName());
                    m.setBizDate(order.getBizDate() == null ? null : order.getBizDate().toString());
                    m.setStatus(order.getStatus());
                    m.setSpdid(item.getSpdid());
                    m.setQty(item.getQty() == null ? null : item.getQty().toPlainString());
                    Drug drug = item.getDrugId() == null ? null : drugService.getById(item.getDrugId());
                    m.setDrugName(drug == null ? vo.getDrugName() : drug.getGenericName());
                    m.setBatchNo(item.getBatchNo());
                    vo.getOutbounds().add(m);
                }
            }
        }
    }
}
