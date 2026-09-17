package com.sunlee.bus.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.common.PharmaAmounts;
import com.sunlee.bus.common.PharmaNos;
import com.sunlee.bus.entity.Customer;
import com.sunlee.bus.entity.Drug;
import com.sunlee.bus.entity.MakerFlowAccount;
import com.sunlee.bus.entity.OpsFlowDoc;
import com.sunlee.bus.entity.OpsFlowItem;
import com.sunlee.bus.entity.SalesOrder;
import com.sunlee.bus.entity.SalesOrderItem;
import com.sunlee.bus.entity.Supplier;
import com.sunlee.bus.entity.Warehouse;
import com.sunlee.bus.mapper.MakerFlowAccountMapper;
import com.sunlee.bus.mapper.OpsFlowDocMapper;
import com.sunlee.bus.mapper.OpsFlowItemMapper;
import com.sunlee.bus.service.IBatchStockService;
import com.sunlee.bus.service.ICustomerService;
import com.sunlee.bus.service.IDrugService;
import com.sunlee.bus.service.IOpsFlowService;
import com.sunlee.bus.service.ISalesOrderItemService;
import com.sunlee.bus.service.ISalesOrderService;
import com.sunlee.bus.service.ISupplierService;
import com.sunlee.bus.service.IWarehouseService;
import com.sunlee.bus.vo.OpsFlowDocVo;
import com.sunlee.sys.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpsFlowServiceImpl extends ServiceImpl<OpsFlowDocMapper, OpsFlowDoc> implements IOpsFlowService {

    @Autowired
    private OpsFlowItemMapper itemMapper;

    @Autowired
    private MakerFlowAccountMapper makerFlowAccountMapper;

    @Autowired
    private ICustomerService customerService;

    @Autowired
    private ISupplierService supplierService;

    @Autowired
    private IDrugService drugService;

    @Autowired
    private IWarehouseService warehouseService;

    @Autowired
    private IBatchStockService batchStockService;

    @Autowired
    private ISalesOrderService salesOrderService;

    @Autowired
    private ISalesOrderItemService salesOrderItemService;

    @Override
    public IPage<OpsFlowDoc> pageDocs(OpsFlowDocVo vo) {
        IPage<OpsFlowDoc> page = new Page<>(vo.getPage(), vo.getLimit());
        QueryWrapper<OpsFlowDoc> qw = new QueryWrapper<>();
        qw.eq(StringUtils.isNotBlank(vo.getDocType()), "doc_type", vo.getDocType());
        qw.like(StringUtils.isNotBlank(vo.getDocNo()), "doc_no", vo.getDocNo());
        qw.eq(StringUtils.isNotBlank(vo.getStatus()), "status", vo.getStatus());
        qw.eq(vo.getCustomerId() != null, "customer_id", vo.getCustomerId());
        qw.eq(vo.getSupplierId() != null, "supplier_id", vo.getSupplierId());
        qw.orderByDesc("id");
        this.page(page, qw);
        page.getRecords().forEach(this::fillNames);
        return page;
    }

    @Override
    public OpsFlowDoc getDetail(Long id) {
        OpsFlowDoc doc = this.getById(id);
        if (doc == null) {
            throw new IllegalArgumentException("单据不存在");
        }
        fillNames(doc);
        List<OpsFlowItem> items = itemMapper.selectList(new QueryWrapper<OpsFlowItem>().eq("doc_id", id));
        items.forEach(this::fillItemName);
        doc.setItems(items);
        return doc;
    }

    @Override
    @Transactional
    public OpsFlowDoc saveDraft(OpsFlowDocVo vo) {
        if (StringUtils.isBlank(vo.getDocType())) {
            throw new IllegalArgumentException("缺少单据类型");
        }
        if (vo.getBizDate() == null) {
            vo.setBizDate(LocalDate.now());
        }
        if (StringUtils.isBlank(vo.getStatus())) {
            vo.setStatus(defaultDraftStatus(vo.getDocType()));
        }
        if (StringUtils.isBlank(vo.getDocNo())) {
            vo.setDocNo(PharmaNos.orderNo(prefix(vo.getDocType())));
        }
        User user = currentUser();
        if (vo.getId() == null) {
            vo.setCreatedBy(user == null ? null : user.getId().longValue());
            vo.setCreatedAt(new Date());
        }
        vo.setUpdatedAt(new Date());
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal qty = BigDecimal.ZERO;
        if (vo.getItems() != null) {
            for (OpsFlowItem item : vo.getItems()) {
                if (item.getAmount() == null && item.getQty() != null && item.getPrice() != null) {
                    item.setAmount(PharmaAmounts.lineAmount(item.getQty(), item.getPrice()));
                }
                if (item.getAmount() != null) {
                    total = total.add(item.getAmount());
                }
                if (item.getQty() != null) {
                    qty = qty.add(item.getQty());
                }
            }
        }
        if (vo.getAmount() == null) {
            vo.setAmount(total);
        }
        if (vo.getQty() == null) {
            vo.setQty(qty);
        }
        this.saveOrUpdate(vo);
        itemMapper.delete(new QueryWrapper<OpsFlowItem>().eq("doc_id", vo.getId()));
        if (vo.getItems() != null) {
            for (OpsFlowItem item : vo.getItems()) {
                item.setId(null);
                item.setDocId(vo.getId());
                item.setCreatedAt(new Date());
                itemMapper.insert(item);
            }
        }
        return getDetail(vo.getId());
    }

    @Override
    @Transactional
    public void confirm(Long id, String action) {
        OpsFlowDoc doc = getDetail(id);
        String next = nextStatus(doc, action);
        applySideEffect(doc, next);
        doc.setStatus(next);
        User user = currentUser();
        if (user != null) {
            doc.setConfirmedBy(user.getId().longValue());
        }
        doc.setConfirmedAt(new Date());
        doc.setUpdatedAt(new Date());
        this.updateById(doc);
    }

    @Override
    @Transactional
    public void deleteDraft(Long id) {
        OpsFlowDoc doc = this.getById(id);
        if (doc == null) {
            throw new IllegalArgumentException("单据不存在");
        }
        if (!isDraftLike(doc.getStatus())) {
            throw new IllegalArgumentException("仅未完成单据可删除");
        }
        itemMapper.delete(new QueryWrapper<OpsFlowItem>().eq("doc_id", id));
        this.removeById(id);
    }

    @Override
    public Map<String, Object> workbench() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("stockoutOpen", countType("STOCKOUT", "已登记", "草稿"));
        map.put("inboundExOpen", countType("INBOUND_EX", "待处理", "待复检"));
        map.put("returnOpen", countType("RETURN_NOTICE", "草稿", "已签名", "已释放"));
        map.put("offsetOpen", countType("OFFSET", "草稿"));
        map.put("allocateOpen", countType("ALLOCATE", "待分货"));
        map.put("logisticsOpen", countType("LOGISTICS", "草稿"));
        QueryWrapper<SalesOrder> unpaid = new QueryWrapper<>();
        unpaid.in("paid_status", "未回款", "部分回款");
        unpaid.eq("status", PharmaNos.STATUS_CONFIRMED);
        map.put("unpaidOrders", salesOrderService.count(unpaid));
        QueryWrapper<SalesOrder> pending = new QueryWrapper<>();
        pending.eq("receive_status", PharmaNos.RECEIVE_PENDING);
        map.put("pendingReceipt", salesOrderService.count(pending));
        return map;
    }

    @Override
    public Map<String, Object> printPack(String invoiceNo) {
        if (StringUtils.isBlank(invoiceNo)) {
            throw new IllegalArgumentException("请输入发票号");
        }
        SalesOrder order = salesOrderService.getOne(new QueryWrapper<SalesOrder>().eq("invoice_no", invoiceNo), false);
        if (order == null) {
            throw new IllegalArgumentException("未找到发票 " + invoiceNo);
        }
        List<SalesOrderItem> items = salesOrderItemService.list(new QueryWrapper<SalesOrderItem>().eq("order_id", order.getId()));
        Customer customer = order.getCustomerId() == null ? null : customerService.getById(order.getCustomerId());
        Warehouse warehouse = order.getWarehouseId() == null ? null : warehouseService.getById(order.getWarehouseId());
        Map<String, Object> pack = new LinkedHashMap<>();
        pack.put("orderNo", order.getOrderNo());
        pack.put("invoiceNo", order.getInvoiceNo());
        pack.put("bizDate", order.getBizDate());
        pack.put("customerName", customer == null ? "" : customer.getName());
        pack.put("customerAddress", customer == null ? "" : customer.getAddress());
        pack.put("warehouseName", warehouse == null ? "" : warehouse.getName());
        pack.put("route", "广深惠干线 / 支线-" + (customer == null ? "00" : String.format("%02d", customer.getId() % 12 + 1)));
        pack.put("payType", order.getPayType());
        pack.put("totalAmount", order.getTotalAmount());
        pack.put("status", order.getStatus());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SalesOrderItem item : items) {
            Drug drug = item.getDrugId() == null ? null : drugService.getById(item.getDrugId());
            Map<String, Object> row = new HashMap<>();
            row.put("drugName", drug == null ? "" : drug.getGenericName());
            row.put("spec", drug == null ? "" : drug.getSpec());
            row.put("batchNo", item.getBatchNo());
            row.put("expireDate", item.getExpireDate());
            row.put("qty", item.getQty());
            row.put("price", item.getSalePrice());
            row.put("amount", item.getAmount());
            rows.add(row);
        }
        pack.put("items", rows);
        pack.put("docs", List.of("随货同行单", "签收单", "路单", "发票清单"));
        return pack;
    }

    @Override
    public Map<String, Object> makerQuery(String loginName, String password, String from, String to) {
        MakerFlowAccount acc = makerFlowAccountMapper.selectOne(
                new QueryWrapper<MakerFlowAccount>().eq("login_name", loginName).eq("status", 1), false);
        if (acc == null || acc.getPassword() == null || !acc.getPassword().equals(password)) {
            throw new IllegalArgumentException("流向查询账号或密码错误");
        }
        if (acc.getEffectiveAt() != null && acc.getEffectiveAt().after(new Date())) {
            throw new IllegalArgumentException("账号次日生效，请明天再查");
        }
        QueryWrapper<SalesOrderItem> itemQw = new QueryWrapper<>();
        List<SalesOrderItem> items = salesOrderItemService.list(itemQw);
        List<Map<String, Object>> flows = new ArrayList<>();
        for (SalesOrderItem item : items) {
            Drug drug = item.getDrugId() == null ? null : drugService.getById(item.getDrugId());
            if (drug == null) {
                continue;
            }
            if (acc.getSupplierId() != null && acc.getSupplierId() != 200001L) {
                Supplier supplier = supplierService.getById(acc.getSupplierId());
                String maker = drug.getManufacturer() == null ? "" : drug.getManufacturer();
                String sname = supplier == null || supplier.getName() == null ? "" : supplier.getName();
                if (!maker.contains("广州") && (sname.length() < 2 || !maker.contains(sname.substring(0, 2)))) {
                    continue;
                }
            }
            SalesOrder order = salesOrderService.getById(item.getOrderId());
            if (order == null || !PharmaNos.STATUS_CONFIRMED.equals(order.getStatus())) {
                continue;
            }
            if (StringUtils.isNotBlank(from) && order.getBizDate() != null && order.getBizDate().isBefore(LocalDate.parse(from))) {
                continue;
            }
            if (StringUtils.isNotBlank(to) && order.getBizDate() != null && order.getBizDate().isAfter(LocalDate.parse(to))) {
                continue;
            }
            Customer customer = order.getCustomerId() == null ? null : customerService.getById(order.getCustomerId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bizDate", order.getBizDate());
            row.put("invoiceNo", order.getInvoiceNo());
            row.put("customerName", customer == null ? "" : customer.getName());
            row.put("drugName", drug.getGenericName());
            row.put("spec", drug.getSpec());
            row.put("batchNo", item.getBatchNo());
            row.put("qty", item.getQty());
            row.put("manufacturer", drug.getManufacturer());
            row.put("flowStatus", order.getBizDate() != null && order.getBizDate().isBefore(LocalDate.now()) ? "已发布" : "次日可见");
            flows.add(row);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("supplierName", acc.getSupplierId() == null ? "" : nameOfSupplier(acc.getSupplierId()));
        res.put("loginName", loginName);
        res.put("count", flows.size());
        res.put("flows", flows);
        return res;
    }

    @Override
    public List<Map<String, Object>> makerAccounts() {
        List<MakerFlowAccount> list = makerFlowAccountMapper.selectList(new QueryWrapper<MakerFlowAccount>().orderByDesc("id"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MakerFlowAccount a : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("loginName", a.getLoginName());
            m.put("supplierName", nameOfSupplier(a.getSupplierId()));
            m.put("status", a.getStatus());
            m.put("effectiveAt", a.getEffectiveAt());
            m.put("remark", a.getRemark());
            rows.add(m);
        }
        return rows;
    }

    private void applySideEffect(OpsFlowDoc doc, String next) {
        if ("RETURN_NOTICE".equals(doc.getDocType()) && "已入库".equals(next)) {
            for (OpsFlowItem item : doc.getItems()) {
                if (item.getDrugId() == null || doc.getWarehouseId() == null || item.getQty() == null) {
                    continue;
                }
                batchStockService.increase(item.getDrugId(), doc.getWarehouseId(),
                        StringUtils.defaultIfBlank(item.getBatchNo(), "RTN"),
                        StringUtils.defaultIfBlank(item.getQualityStatus(), "待验"),
                        item.getQty(), null, item.getExpireDate());
            }
        }
        if ("OFFSET".equals(doc.getDocType()) && "已冲账".equals(next) && StringUtils.isNotBlank(doc.getRelatedNo())) {
            SalesOrder order = salesOrderService.getOne(new QueryWrapper<SalesOrder>()
                    .eq("invoice_no", doc.getRelatedNo())
                    .or().eq("order_no", doc.getRelatedNo()), false);
            if (order != null) {
                BigDecimal pay = doc.getAmount() == null ? BigDecimal.ZERO : doc.getAmount();
                BigDecimal already = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
                BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
                BigDecimal now = already.add(pay);
                order.setPaidAmount(now);
                order.setPaidAt(new Date());
                if (now.compareTo(total) >= 0) {
                    order.setPaidStatus(PharmaNos.PAID_DONE);
                } else if (now.compareTo(BigDecimal.ZERO) > 0) {
                    order.setPaidStatus(PharmaNos.PAID_PARTIAL);
                }
                salesOrderService.updateById(order);
            }
        }
    }

    private String nextStatus(OpsFlowDoc doc, String action) {
        String type = doc.getDocType();
        String cur = doc.getStatus();
        if (StringUtils.isNotBlank(action)) {
            return action;
        }
        return switch (type) {
            case "STOCKOUT" -> "已补货";
            case "INBOUND_EX" -> "待复检".equals(cur) ? "合格" : "待复检";
            case "RETURN_NOTICE" -> switch (cur) {
                case "草稿" -> "已签名";
                case "已签名" -> "已释放";
                default -> "已入库";
            };
            case "OFFSET" -> "已冲账";
            case "ALLOCATE" -> "已分货";
            case "LOGISTICS" -> "已同意";
            case "CREDIT" -> "已生效";
            case "QUOTA" -> "已生效";
            case "PRINT_PACK" -> "已打印";
            case "FLOW" -> "已发布";
            default -> "已确认";
        };
    }

    private boolean isDraftLike(String status) {
        return status == null || List.of("草稿", "待处理", "待分货", "已登记", "已签名").contains(status);
    }

    private String defaultDraftStatus(String type) {
        return switch (type) {
            case "INBOUND_EX" -> "待处理";
            case "ALLOCATE" -> "待分货";
            case "STOCKOUT" -> "已登记";
            default -> "草稿";
        };
    }

    private String prefix(String type) {
        return switch (type) {
            case "STOCKOUT" -> "QH";
            case "INBOUND_EX" -> "YC";
            case "RETURN_NOTICE" -> "XT";
            case "OFFSET" -> "CZ";
            case "ALLOCATE" -> "FH";
            case "LOGISTICS" -> "WL";
            case "CREDIT" -> "XY";
            case "QUOTA" -> "TC";
            case "PRINT_PACK" -> "DY";
            case "FLOW" -> "LX";
            default -> "YW";
        };
    }

    private long countType(String type, String... statuses) {
        QueryWrapper<OpsFlowDoc> qw = new QueryWrapper<>();
        qw.eq("doc_type", type);
        if (statuses != null && statuses.length > 0) {
            qw.in("status", (Object[]) statuses);
        }
        return this.count(qw);
    }

    private void fillNames(OpsFlowDoc doc) {
        if (doc.getCustomerId() != null) {
            Customer c = customerService.getById(doc.getCustomerId());
            if (c != null) {
                doc.setCustomerName(c.getName());
            }
        }
        if (doc.getSupplierId() != null) {
            doc.setSupplierName(nameOfSupplier(doc.getSupplierId()));
        }
        if (doc.getDrugId() != null) {
            Drug d = drugService.getById(doc.getDrugId());
            if (d != null) {
                doc.setDrugName(d.getGenericName());
            }
        }
        if (doc.getWarehouseId() != null) {
            Warehouse w = warehouseService.getById(doc.getWarehouseId());
            if (w != null) {
                doc.setWarehouseName(w.getName());
            }
        }
    }

    private void fillItemName(OpsFlowItem item) {
        if (item.getDrugId() != null) {
            Drug d = drugService.getById(item.getDrugId());
            if (d != null) {
                item.setDrugName(d.getGenericName());
            }
        }
    }

    private String nameOfSupplier(Long id) {
        if (id == null) {
            return "";
        }
        Supplier s = supplierService.getById(id);
        return s == null ? "" : s.getName();
    }

    private User currentUser() {
        try {
            return (User) StpUtil.getSession().get("user");
        } catch (Exception e) {
            return null;
        }
    }
}
