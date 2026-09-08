package com.sunlee.bus.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sunlee.bus.common.PharmaNos;
import com.sunlee.bus.entity.Customer;
import com.sunlee.bus.entity.MonthlyCloseRecord;
import com.sunlee.bus.entity.PurchaseOrder;
import com.sunlee.bus.entity.SalesOrder;
import com.sunlee.bus.entity.Supplier;
import com.sunlee.bus.mapper.MonthlyCloseRecordMapper;
import com.sunlee.bus.service.ICustomerService;
import com.sunlee.bus.service.IMonthlyCloseService;
import com.sunlee.bus.service.IPurchaseOrderService;
import com.sunlee.bus.service.ISalesOrderService;
import com.sunlee.bus.service.ISupplierService;
import com.sunlee.bus.vo.MonthlyCloseStatement;
import com.sunlee.bus.vo.MonthlyCloseStatement.PartnerRow;
import com.sunlee.bus.vo.MonthlyCloseStatement.Summary;
import com.sunlee.sys.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MonthlyCloseServiceImpl implements IMonthlyCloseService {

    @Autowired
    private MonthlyCloseRecordMapper monthlyCloseRecordMapper;

    @Autowired
    private ISalesOrderService salesOrderService;

    @Autowired
    private IPurchaseOrderService purchaseOrderService;

    @Autowired
    private ICustomerService customerService;

    @Autowired
    private ISupplierService supplierService;

    @Override
    public MonthlyCloseStatement loadStatement(String yearMonth) {
        YearMonth ym = parseYearMonth(yearMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        MonthlyCloseStatement result = new MonthlyCloseStatement();
        result.setYearMonth(ym.toString());

        QueryWrapper<SalesOrder> salesQw = new QueryWrapper<>();
        salesQw.eq("status", PharmaNos.STATUS_CONFIRMED);
        salesQw.ge("biz_date", start);
        salesQw.le("biz_date", end);
        salesQw.and(w -> w.isNull("order_type").or().ne("order_type", PharmaNos.ORDER_REVERSAL));
        salesQw.orderByAsc("customer_id", "id");
        Map<Long, PartnerRow> customers = new LinkedHashMap<>();
        for (SalesOrder order : salesOrderService.list(salesQw)) {
            PartnerRow row = customers.computeIfAbsent(order.getCustomerId(), id -> {
                PartnerRow created = new PartnerRow();
                created.setPartnerId(id);
                created.setPartnerName(customerName(id));
                created.setPayType(StringUtils.defaultIfBlank(order.getPayType(), PharmaNos.PAY_MONTH));
                return created;
            });
            row.setOrderCount(row.getOrderCount() + 1);
            BigDecimal total = nz(order.getTotalAmount());
            BigDecimal paid = paidAmount(order);
            row.setTotalAmount(row.getTotalAmount().add(total));
            row.setPaidAmount(row.getPaidAmount().add(paid));
            row.setUnpaidAmount(row.getUnpaidAmount().add(total.subtract(paid).max(BigDecimal.ZERO)));
        }
        result.getCustomers().addAll(customers.values());

        QueryWrapper<PurchaseOrder> purchaseQw = new QueryWrapper<>();
        purchaseQw.eq("status", PharmaNos.STATUS_CONFIRMED);
        purchaseQw.ge("biz_date", start);
        purchaseQw.le("biz_date", end);
        purchaseQw.orderByAsc("supplier_id", "id");
        Map<Long, PartnerRow> suppliers = new LinkedHashMap<>();
        for (PurchaseOrder order : purchaseOrderService.list(purchaseQw)) {
            PartnerRow row = suppliers.computeIfAbsent(order.getSupplierId(), id -> {
                PartnerRow created = new PartnerRow();
                created.setPartnerId(id);
                created.setPartnerName(supplierName(id));
                return created;
            });
            row.setOrderCount(row.getOrderCount() + 1);
            row.setTotalAmount(row.getTotalAmount().add(nz(order.getTotalAmount())));
        }
        result.getSuppliers().addAll(suppliers.values());

        Summary summary = result.getSummary();
        summary.setCustomerCount(result.getCustomers().size());
        summary.setSupplierCount(result.getSuppliers().size());
        for (PartnerRow row : result.getCustomers()) {
            summary.setSalesAmount(summary.getSalesAmount().add(row.getTotalAmount()));
            summary.setPaidAmount(summary.getPaidAmount().add(row.getPaidAmount()));
            summary.setUnpaidAmount(summary.getUnpaidAmount().add(row.getUnpaidAmount()));
        }
        for (PartnerRow row : result.getSuppliers()) {
            summary.setPurchaseAmount(summary.getPurchaseAmount().add(row.getTotalAmount()));
        }

        QueryWrapper<MonthlyCloseRecord> recQw = new QueryWrapper<>();
        recQw.eq("close_month", ym.toString());
        recQw.last("LIMIT 1");
        MonthlyCloseRecord record = monthlyCloseRecordMapper.selectOne(recQw);
        if (record != null) {
            result.setClosed(true);
            result.setClosedAt(record.getClosedAt());
            result.setClosedByName(record.getClosedByName());
        }
        return result;
    }

    @Override
    @Transactional
    public MonthlyCloseRecord confirm(String yearMonth) {
        MonthlyCloseStatement statement = loadStatement(yearMonth);
        if (statement.isClosed()) {
            throw new IllegalArgumentException(statement.getYearMonth() + " 已经月结");
        }
        Date now = new Date();
        User user = currentUser();
        MonthlyCloseRecord record = new MonthlyCloseRecord();
        record.setYearMonth(statement.getYearMonth());
        record.setStatus("已月结");
        record.setCustomerCount(statement.getSummary().getCustomerCount());
        record.setSalesAmount(statement.getSummary().getSalesAmount());
        record.setUnpaidAmount(statement.getSummary().getUnpaidAmount());
        record.setSupplierCount(statement.getSummary().getSupplierCount());
        record.setPurchaseAmount(statement.getSummary().getPurchaseAmount());
        record.setClosedBy(user == null ? null : user.getId().longValue());
        record.setClosedByName(user == null ? null : user.getName());
        record.setClosedAt(now);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        monthlyCloseRecordMapper.insert(record);
        return record;
    }

    private BigDecimal paidAmount(SalesOrder order) {
        if (order.getPaidAmount() != null) {
            return order.getPaidAmount();
        }
        if (PharmaNos.PAID_DONE.equals(order.getPaidStatus())) {
            return nz(order.getTotalAmount());
        }
        return BigDecimal.ZERO;
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

    private User currentUser() {
        try {
            return (User) StpUtil.getSession().get("user");
        } catch (Exception e) {
            return null;
        }
    }

    private static YearMonth parseYearMonth(String yearMonth) {
        if (StringUtils.isBlank(yearMonth)) {
            return YearMonth.now();
        }
        return YearMonth.parse(yearMonth);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
