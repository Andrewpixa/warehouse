package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sunlee.bus.common.PharmaNos;
import com.sunlee.bus.entity.Drug;
import com.sunlee.bus.entity.PurchaseOrder;
import com.sunlee.bus.entity.PurchaseOrderItem;
import com.sunlee.bus.entity.SalesOrder;
import com.sunlee.bus.entity.SalesOrderItem;
import com.sunlee.bus.service.IDrugService;
import com.sunlee.bus.service.IPharmaStatsService;
import com.sunlee.bus.service.IPurchaseOrderItemService;
import com.sunlee.bus.service.IPurchaseOrderService;
import com.sunlee.bus.service.ISalesOrderItemService;
import com.sunlee.bus.service.ISalesOrderService;
import com.sunlee.bus.vo.PharmaStatsResult;
import com.sunlee.bus.vo.PharmaStatsResult.DayRow;
import com.sunlee.bus.vo.PharmaStatsResult.DrugRow;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PharmaStatsServiceImpl implements IPharmaStatsService {

    @Autowired
    private IPurchaseOrderService purchaseOrderService;

    @Autowired
    private IPurchaseOrderItemService purchaseOrderItemService;

    @Autowired
    private ISalesOrderService salesOrderService;

    @Autowired
    private ISalesOrderItemService salesOrderItemService;

    @Autowired
    private IDrugService drugService;

    @Override
    public PharmaStatsResult purchaseStats(String startDate, String endDate) {
        LocalDate start = parseStart(startDate);
        LocalDate end = parseEnd(endDate);
        QueryWrapper<PurchaseOrder> qw = new QueryWrapper<>();
        qw.eq("status", PharmaNos.STATUS_CONFIRMED);
        qw.ge("biz_date", start);
        qw.le("biz_date", end);
        qw.orderByAsc("biz_date", "id");
        List<PurchaseOrder> orders = purchaseOrderService.list(qw);
        Map<Long, PurchaseOrder> orderMap = orders.stream()
                .collect(Collectors.toMap(PurchaseOrder::getId, o -> o, (a, b) -> a));
        List<ItemLine> lines = new java.util.ArrayList<>();
        if (!orderMap.isEmpty()) {
            QueryWrapper<PurchaseOrderItem> itemQw = new QueryWrapper<>();
            itemQw.in("order_id", orderMap.keySet());
            for (PurchaseOrderItem item : purchaseOrderItemService.list(itemQw)) {
                PurchaseOrder order = orderMap.get(item.getOrderId());
                if (order == null) {
                    continue;
                }
                lines.add(new ItemLine(order.getBizDate(), item.getDrugId(), nz(item.getStockInQty()), nz(item.getAmount())));
            }
        }
        Map<LocalDate, Long> dayCounts = orders.stream()
                .collect(Collectors.groupingBy(PurchaseOrder::getBizDate, Collectors.counting()));
        Map<LocalDate, BigDecimal> dayAmounts = orders.stream()
                .collect(Collectors.groupingBy(PurchaseOrder::getBizDate,
                        Collectors.reducing(BigDecimal.ZERO, o -> nz(o.getTotalAmount()), BigDecimal::add)));
        return assemble(start, end, orders.size(), dayCounts, dayAmounts, lines);
    }

    @Override
    public PharmaStatsResult salesStats(String startDate, String endDate) {
        LocalDate start = parseStart(startDate);
        LocalDate end = parseEnd(endDate);
        QueryWrapper<SalesOrder> qw = new QueryWrapper<>();
        qw.eq("status", PharmaNos.STATUS_CONFIRMED);
        qw.ge("biz_date", start);
        qw.le("biz_date", end);
        qw.and(w -> w.isNull("order_type").or().ne("order_type", PharmaNos.ORDER_REVERSAL));
        qw.orderByAsc("biz_date", "id");
        List<SalesOrder> orders = salesOrderService.list(qw);
        Map<Long, SalesOrder> orderMap = orders.stream()
                .collect(Collectors.toMap(SalesOrder::getId, o -> o, (a, b) -> a));
        List<ItemLine> lines = new java.util.ArrayList<>();
        if (!orderMap.isEmpty()) {
            QueryWrapper<SalesOrderItem> itemQw = new QueryWrapper<>();
            itemQw.in("order_id", orderMap.keySet());
            for (SalesOrderItem item : salesOrderItemService.list(itemQw)) {
                SalesOrder order = orderMap.get(item.getOrderId());
                if (order == null) {
                    continue;
                }
                lines.add(new ItemLine(order.getBizDate(), item.getDrugId(), nz(item.getQty()), nz(item.getAmount())));
            }
        }
        Map<LocalDate, Long> dayCounts = orders.stream()
                .collect(Collectors.groupingBy(SalesOrder::getBizDate, Collectors.counting()));
        Map<LocalDate, BigDecimal> dayAmounts = orders.stream()
                .collect(Collectors.groupingBy(SalesOrder::getBizDate,
                        Collectors.reducing(BigDecimal.ZERO, o -> nz(o.getTotalAmount()), BigDecimal::add)));
        return assemble(start, end, orders.size(), dayCounts, dayAmounts, lines);
    }

    private PharmaStatsResult assemble(LocalDate start, LocalDate end, long orderCount,
                                       Map<LocalDate, Long> orderCountByDay,
                                       Map<LocalDate, BigDecimal> amountByDay,
                                       List<ItemLine> lines) {
        PharmaStatsResult result = new PharmaStatsResult();
        result.setStartDate(start.toString());
        result.setEndDate(end.toString());
        Map<LocalDate, DayRow> days = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayRow row = new DayRow();
            row.setBizDate(d.toString());
            row.setOrderCount(orderCountByDay.getOrDefault(d, 0L));
            row.setAmount(amountByDay.getOrDefault(d, BigDecimal.ZERO));
            days.put(d, row);
        }
        Map<Long, DrugRow> drugs = new LinkedHashMap<>();
        for (ItemLine line : lines) {
            DayRow day = days.get(line.bizDate);
            if (day != null) {
                day.setQty(day.getQty().add(line.qty));
            }
            DrugRow drugRow = drugs.computeIfAbsent(line.drugId, id -> {
                DrugRow created = new DrugRow();
                created.setDrugId(id);
                Drug drug = id == null ? null : drugService.getById(id);
                created.setDrugName(drug == null ? String.valueOf(id) : drug.getGenericName());
                return created;
            });
            drugRow.setQty(drugRow.getQty().add(line.qty));
            drugRow.setAmount(drugRow.getAmount().add(line.amount));
        }
        result.getSummary().setOrderCount(orderCount);
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (DayRow row : days.values()) {
            result.getDays().add(row);
            totalQty = totalQty.add(row.getQty());
            totalAmount = totalAmount.add(row.getAmount());
        }
        result.getSummary().setQty(totalQty);
        result.getSummary().setAmount(totalAmount);
        result.getDrugs().addAll(drugs.values());
        result.getDrugs().sort(Comparator.comparing(DrugRow::getAmount).reversed());
        return result;
    }

    private static LocalDate parseStart(String startDate) {
        if (StringUtils.isBlank(startDate)) {
            return LocalDate.now().withDayOfMonth(1);
        }
        return LocalDate.parse(startDate);
    }

    private static LocalDate parseEnd(String endDate) {
        if (StringUtils.isBlank(endDate)) {
            return LocalDate.now();
        }
        return LocalDate.parse(endDate);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static class ItemLine {
        private final LocalDate bizDate;
        private final Long drugId;
        private final BigDecimal qty;
        private final BigDecimal amount;

        private ItemLine(LocalDate bizDate, Long drugId, BigDecimal qty, BigDecimal amount) {
            this.bizDate = bizDate;
            this.drugId = drugId;
            this.qty = qty;
            this.amount = amount;
        }
    }
}
