package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sunlee.bus.common.PharmaNos;
import cn.dev33.satoken.stp.StpUtil;
import com.sunlee.bus.entity.Customer;
import com.sunlee.bus.entity.DailyCloseRecord;
import com.sunlee.bus.entity.Drug;
import com.sunlee.bus.entity.PurchaseOrder;
import com.sunlee.bus.entity.PurchaseOrderItem;
import com.sunlee.bus.entity.SalesOrder;
import com.sunlee.bus.entity.SalesOrderItem;
import com.sunlee.bus.entity.Supplier;
import com.sunlee.bus.entity.SurplusOrder;
import com.sunlee.bus.entity.TraceCode;
import com.sunlee.bus.entity.Warehouse;
import com.sunlee.bus.mapper.DailyCloseRecordMapper;
import com.sunlee.bus.service.ICustomerService;
import com.sunlee.bus.service.IDailyCloseService;
import com.sunlee.bus.service.IDrugService;
import com.sunlee.bus.service.IPurchaseOrderItemService;
import com.sunlee.bus.service.IPurchaseOrderService;
import com.sunlee.bus.service.IBizVoucherService;
import com.sunlee.bus.service.ISalesOrderItemService;
import com.sunlee.bus.service.ISalesOrderService;
import com.sunlee.bus.service.ISupplierService;
import com.sunlee.bus.service.ISurplusOrderService;
import com.sunlee.bus.service.ITraceCodeService;
import com.sunlee.bus.service.IWarehouseService;
import com.sunlee.bus.vo.DailyCloseChecklist;
import com.sunlee.bus.vo.DailyCloseChecklist.OrderRow;
import com.sunlee.bus.vo.DailyCloseChecklist.Summary;
import com.sunlee.bus.vo.DailyCloseChecklist.TraceRow;
import com.sunlee.bus.vo.VoucherIssue;
import com.sunlee.sys.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DailyCloseServiceImpl implements IDailyCloseService {

    @Autowired
    private IPurchaseOrderService purchaseOrderService;

    @Autowired
    private ISalesOrderService salesOrderService;

    @Autowired
    private ISalesOrderItemService salesOrderItemService;

    @Autowired
    private ISurplusOrderService surplusOrderService;

    @Autowired
    private ITraceCodeService traceCodeService;

    @Autowired
    private IDrugService drugService;

    @Autowired
    private ICustomerService customerService;

    @Autowired
    private ISupplierService supplierService;

    @Autowired
    private IWarehouseService warehouseService;

    @Autowired
    private DailyCloseRecordMapper dailyCloseRecordMapper;

    @Autowired
    private IBizVoucherService voucherService;

    @Autowired
    private IPurchaseOrderItemService purchaseOrderItemService;

    @Override
    public DailyCloseChecklist loadChecklist(LocalDate bizDate) {
        if (bizDate == null) {
            bizDate = LocalDate.now();
        }
        DailyCloseChecklist result = new DailyCloseChecklist();
        result.setBizDate(bizDate);
        Summary summary = result.getSummary();

        List<PurchaseOrder> purchases = listByDate(purchaseOrderService, bizDate);
        List<SalesOrder> sales = listSalesByDate(bizDate);
        List<SurplusOrder> surplus = listByDate(surplusOrderService, bizDate);

        List<PurchaseOrder> confirmedPurchases = new ArrayList<>();
        for (PurchaseOrder order : purchases) {
            if (PharmaNos.STATUS_DRAFT.equals(order.getStatus())) {
                result.getDraftPurchases().add(toPurchaseRow(order));
            } else if (PharmaNos.STATUS_CONFIRMED.equals(order.getStatus())) {
                confirmedPurchases.add(order);
                summary.setConfirmedPurchaseCount(summary.getConfirmedPurchaseCount() + 1);
                summary.setConfirmedPurchaseAmount(add(summary.getConfirmedPurchaseAmount(), order.getTotalAmount()));
            }
        }

        List<SalesOrder> confirmedSales = new ArrayList<>();
        for (SalesOrder order : sales) {
            if (PharmaNos.STATUS_DRAFT.equals(order.getStatus())) {
                result.getDraftOutbounds().add(toSalesRow(order));
            } else if (PharmaNos.STATUS_CONFIRMED.equals(order.getStatus())) {
                    if (!isReversal(order)) {
                    confirmedSales.add(order);
                    summary.setConfirmedOutboundCount(summary.getConfirmedOutboundCount() + 1);
                    summary.setConfirmedOutboundAmount(add(summary.getConfirmedOutboundAmount(), order.getTotalAmount()));
                    if (isPendingReceive(order)) {
                        result.getPendingReceipts().add(toSalesRow(order));
                    }
                    if (isCashPay(order) && isOpenPaid(order)) {
                        result.getCashUnpaid().add(toSalesRow(order));
                    } else if (!isCashPay(order) && isOpenPaid(order)) {
                        result.getMonthlyUnpaid().add(toSalesRow(order));
                    }
                }
            }
        }

        for (SurplusOrder order : surplus) {
            if (PharmaNos.STATUS_DRAFT.equals(order.getStatus())) {
                result.getDraftSurplus().add(toSurplusRow(order));
            }
        }

        result.setMissingTraces(findMissingTraces(confirmedSales));
        collectVoucherIssues(result, confirmedPurchases, confirmedSales);

        summary.setDraftPurchaseCount(result.getDraftPurchases().size());
        summary.setDraftOutboundCount(result.getDraftOutbounds().size());
        summary.setDraftSurplusCount(result.getDraftSurplus().size());
        summary.setMissingTraceCount(result.getMissingTraces().size());
        summary.setCashUnpaidCount(result.getCashUnpaid().size());
        summary.setMonthlyUnpaidCount(result.getMonthlyUnpaid().size());
        summary.setPendingReceiptCount(result.getPendingReceipts().size());
        summary.setMissingTicketCount(result.getMissingTickets().size());
        summary.setMissingSignCount(result.getMissingSigns().size());
        summary.setAmountMismatchCount(result.getAmountMismatches().size());

        int blockers = (int) (summary.getDraftPurchaseCount()
                + summary.getDraftOutboundCount()
                + summary.getDraftSurplusCount()
                + summary.getMissingTraceCount()
                + summary.getCashUnpaidCount()
                + summary.getMissingTicketCount()
                + summary.getMissingSignCount()
                + summary.getAmountMismatchCount());
        result.setBlockerCount(blockers);
        result.setCleared(blockers == 0);
        fillClosed(result, bizDate);
        return result;
    }

    @Override
    @Transactional
    public DailyCloseRecord confirm(LocalDate bizDate) {
        DailyCloseChecklist checklist = loadChecklist(bizDate);
        if (checklist.isClosed()) {
            throw new IllegalArgumentException(checklist.getBizDate() + " 已经日清，无需重复操作");
        }
        if (!checklist.isCleared()) {
            throw new IllegalArgumentException("还有 " + checklist.getBlockerCount() + " 项未清，不能日清");
        }
        Date now = new Date();
        User user = currentUser();
        DailyCloseRecord record = new DailyCloseRecord();
        record.setBizDate(checklist.getBizDate());
        record.setStatus("已日清");
        record.setPurchaseCount((int) checklist.getSummary().getConfirmedPurchaseCount());
        record.setPurchaseAmount(checklist.getSummary().getConfirmedPurchaseAmount());
        record.setOutboundCount((int) checklist.getSummary().getConfirmedOutboundCount());
        record.setOutboundAmount(checklist.getSummary().getConfirmedOutboundAmount());
        record.setClosedBy(user == null ? null : user.getId().longValue());
        record.setClosedByName(user == null ? null : user.getName());
        record.setClosedAt(now);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        dailyCloseRecordMapper.insert(record);
        return record;
    }

    private void fillClosed(DailyCloseChecklist result, LocalDate bizDate) {
        QueryWrapper<DailyCloseRecord> qw = new QueryWrapper<>();
        qw.eq("biz_date", bizDate);
        qw.last("LIMIT 1");
        DailyCloseRecord record = dailyCloseRecordMapper.selectOne(qw);
        if (record != null) {
            result.setClosed(true);
            result.setClosedAt(record.getClosedAt());
            result.setClosedByName(record.getClosedByName());
        }
    }

    private User currentUser() {
        try {
            return (User) StpUtil.getSession().get("user");
        } catch (Exception e) {
            return null;
        }
    }

    private List<PurchaseOrder> listByDate(IPurchaseOrderService service, LocalDate bizDate) {
        QueryWrapper<PurchaseOrder> qw = new QueryWrapper<>();
        qw.eq("biz_date", bizDate);
        qw.orderByAsc("id");
        return service.list(qw);
    }

    private List<SurplusOrder> listByDate(ISurplusOrderService service, LocalDate bizDate) {
        QueryWrapper<SurplusOrder> qw = new QueryWrapper<>();
        qw.eq("biz_date", bizDate);
        qw.orderByAsc("id");
        return service.list(qw);
    }

    private List<SalesOrder> listSalesByDate(LocalDate bizDate) {
        QueryWrapper<SalesOrder> qw = new QueryWrapper<>();
        qw.eq("biz_date", bizDate);
        qw.orderByAsc("id");
        return salesOrderService.list(qw);
    }

    private List<TraceRow> findMissingTraces(List<SalesOrder> confirmedSales) {
        List<TraceRow> missing = new ArrayList<>();
        if (confirmedSales.isEmpty()) {
            return missing;
        }
        Map<Long, SalesOrder> orderMap = new HashMap<>();
        List<Long> orderIds = new ArrayList<>();
        for (SalesOrder order : confirmedSales) {
            orderMap.put(order.getId(), order);
            orderIds.add(order.getId());
        }
        QueryWrapper<SalesOrderItem> itemQw = new QueryWrapper<>();
        itemQw.in("order_id", orderIds);
        List<SalesOrderItem> items = salesOrderItemService.list(itemQw);
        if (items.isEmpty()) {
            return missing;
        }

        Set<String> spdids = items.stream()
                .map(SalesOrderItem::getSpdid)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        Set<String> coded = new HashSet<>();
        if (!spdids.isEmpty()) {
            QueryWrapper<TraceCode> traceQw = new QueryWrapper<>();
            traceQw.in("spdid", spdids);
            for (TraceCode code : traceCodeService.list(traceQw)) {
                if (code.getSpdid() != null && !"作废".equals(code.getStatus())) {
                    coded.add(code.getSpdid());
                }
            }
        }

        Set<Long> drugIds = items.stream().map(SalesOrderItem::getDrugId).collect(Collectors.toSet());
        Map<Long, Drug> drugs = new HashMap<>();
        if (!drugIds.isEmpty()) {
            for (Drug drug : drugService.listByIds(drugIds)) {
                drugs.put(drug.getId(), drug);
            }
        }

        for (SalesOrderItem item : items) {
            Drug drug = drugs.get(item.getDrugId());
            if (drug != null && !PharmaNos.DRUG_CATEGORY.equals(drug.getCategory())) {
                continue;
            }
            if (StringUtils.isNotBlank(item.getSpdid()) && coded.contains(item.getSpdid())) {
                continue;
            }
            SalesOrder order = orderMap.get(item.getOrderId());
            TraceRow row = new TraceRow();
            row.setOrderId(item.getOrderId());
            if (order != null) {
                row.setOrderNo(order.getOrderNo());
                row.setInvoiceNo(order.getInvoiceNo());
                row.setCustomerName(customerName(order.getCustomerId()));
            }
            row.setDrugName(drug == null ? null : drug.getGenericName());
            row.setBatchNo(item.getBatchNo());
            row.setSpdid(item.getSpdid());
            row.setQty(item.getQty());
            missing.add(row);
        }
        return missing;
    }

    private OrderRow toPurchaseRow(PurchaseOrder order) {
        OrderRow row = new OrderRow();
        row.setId(order.getId());
        row.setOrderNo(order.getOrderNo());
        row.setPartnerName(supplierName(order.getSupplierId()));
        row.setWarehouseName(warehouseName(order.getWarehouseId()));
        row.setTotalAmount(order.getTotalAmount());
        row.setRemark(order.getRemark());
        return row;
    }

    private OrderRow toSalesRow(SalesOrder order) {
        OrderRow row = new OrderRow();
        row.setId(order.getId());
        row.setOrderNo(order.getOrderNo());
        row.setInvoiceNo(order.getInvoiceNo());
        row.setPartnerName(customerName(order.getCustomerId()));
        row.setWarehouseName(warehouseName(order.getWarehouseId()));
        row.setPayType(StringUtils.defaultIfBlank(order.getPayType(), PharmaNos.PAY_MONTH));
        row.setPaidStatus(StringUtils.defaultIfBlank(order.getPaidStatus(), PharmaNos.PAID_NONE));
        row.setTotalAmount(order.getTotalAmount());
        row.setRemark(order.getRemark());
        return row;
    }

    private OrderRow toSurplusRow(SurplusOrder order) {
        OrderRow row = new OrderRow();
        row.setId(order.getId());
        row.setOrderNo(order.getOrderNo());
        row.setWarehouseName(warehouseName(order.getWarehouseId()));
        row.setTotalAmount(order.getTotalSurplusAmount());
        row.setRemark(order.getRemark());
        return row;
    }

    private boolean isReversal(SalesOrder order) {
        return PharmaNos.ORDER_REVERSAL.equals(order.getOrderType());
    }

    private boolean isPendingReceive(SalesOrder order) {
        if (isReversal(order)) {
            return false;
        }
        String status = order.getReceiveStatus();
        return StringUtils.isBlank(status) || PharmaNos.RECEIVE_PENDING.equals(status);
    }

    private boolean isCashPay(SalesOrder order) {
        return PharmaNos.PAY_CASH.equals(order.getPayType());
    }

    private boolean isOpenPaid(SalesOrder order) {
        String status = StringUtils.defaultIfBlank(order.getPaidStatus(), PharmaNos.PAID_NONE);
        return PharmaNos.PAID_NONE.equals(status) || PharmaNos.PAID_PARTIAL.equals(status);
    }

    private String customerName(Long id) {
        if (id == null) {
            return null;
        }
        Customer customer = customerService.getById(id);
        return customer == null ? null : customer.getName();
    }

    private String supplierName(Long id) {
        if (id == null) {
            return null;
        }
        Supplier supplier = supplierService.getById(id);
        return supplier == null ? null : supplier.getName();
    }

    private String warehouseName(Long id) {
        if (id == null) {
            return null;
        }
        Warehouse warehouse = warehouseService.getById(id);
        return warehouse == null ? null : warehouse.getName();
    }

    private void collectVoucherIssues(DailyCloseChecklist result, List<PurchaseOrder> purchases, List<SalesOrder> sales) {
        Map<Long, List<PurchaseOrderItem>> purchaseItems = loadPurchaseItems(purchases);
        for (PurchaseOrder order : purchases) {
            classifyIssues(result, voucherService.inspectPurchase(
                    order, purchaseItems.getOrDefault(order.getId(), List.of()), supplierName(order.getSupplierId())));
        }
        Map<Long, List<SalesOrderItem>> salesItems = loadSalesItems(sales);
        for (SalesOrder order : sales) {
            classifyIssues(result, voucherService.inspectSales(
                    order, salesItems.getOrDefault(order.getId(), List.of()), customerName(order.getCustomerId())));
        }
    }

    private void classifyIssues(DailyCloseChecklist result, List<VoucherIssue> issues) {
        for (VoucherIssue issue : issues) {
            if (VoucherIssue.MISSING_TICKET.equals(issue.getIssueType())) {
                result.getMissingTickets().add(issue);
            } else if (VoucherIssue.MISSING_SIGN.equals(issue.getIssueType())) {
                result.getMissingSigns().add(issue);
            } else {
                result.getAmountMismatches().add(issue);
            }
        }
    }

    private Map<Long, List<PurchaseOrderItem>> loadPurchaseItems(List<PurchaseOrder> orders) {
        Map<Long, List<PurchaseOrderItem>> map = new HashMap<>();
        if (orders.isEmpty()) {
            return map;
        }
        List<Long> ids = orders.stream().map(PurchaseOrder::getId).collect(Collectors.toList());
        QueryWrapper<PurchaseOrderItem> qw = new QueryWrapper<>();
        qw.in("order_id", ids);
        for (PurchaseOrderItem item : purchaseOrderItemService.list(qw)) {
            map.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private Map<Long, List<SalesOrderItem>> loadSalesItems(List<SalesOrder> orders) {
        Map<Long, List<SalesOrderItem>> map = new HashMap<>();
        if (orders.isEmpty()) {
            return map;
        }
        List<Long> ids = orders.stream().map(SalesOrder::getId).collect(Collectors.toList());
        QueryWrapper<SalesOrderItem> qw = new QueryWrapper<>();
        qw.in("order_id", ids);
        for (SalesOrderItem item : salesOrderItemService.list(qw)) {
            map.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private static BigDecimal add(BigDecimal a, BigDecimal b) {
        return (a == null ? BigDecimal.ZERO : a).add(b == null ? BigDecimal.ZERO : b);
    }
}
