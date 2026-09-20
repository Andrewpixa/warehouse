package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.common.PharmaAmounts;
import com.sunlee.bus.common.PharmaNos;
import com.sunlee.bus.entity.BatchStock;
import com.sunlee.bus.entity.Customer;
import com.sunlee.bus.entity.Drug;
import com.sunlee.bus.entity.OpsFlowDoc;
import com.sunlee.bus.entity.SalesOrder;
import com.sunlee.bus.entity.SalesOrderItem;
import com.sunlee.bus.entity.Warehouse;
import com.sunlee.bus.mapper.OpsFlowDocMapper;
import com.sunlee.bus.mapper.SalesOrderMapper;
import com.sunlee.bus.service.IBatchStockService;
import com.sunlee.bus.service.IBizVoucherService;
import com.sunlee.bus.service.ICustomerService;
import com.sunlee.bus.service.IDrugService;
import com.sunlee.bus.service.ISalesOrderItemService;
import com.sunlee.bus.service.ISalesOrderService;
import com.sunlee.bus.service.IWarehouseService;
import com.sunlee.bus.vo.SalesOrderVo;
import com.sunlee.sys.common.AppFileUtils;
import com.sunlee.sys.common.RedisBiz;
import com.sunlee.sys.entity.User;
import cn.dev33.satoken.stp.StpUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesOrderServiceImpl extends ServiceImpl<SalesOrderMapper, SalesOrder> implements ISalesOrderService {

    @Autowired
    private ISalesOrderItemService itemService;

    @Autowired
    private IBatchStockService batchStockService;

    @Autowired
    private ICustomerService customerService;

    @Autowired
    private IWarehouseService warehouseService;

    @Autowired
    private IDrugService drugService;

    @Autowired
    private RedisBiz redisBiz;

    @Autowired
    private IBizVoucherService voucherService;

    @Autowired
    private OpsFlowDocMapper opsFlowDocMapper;

    @Override
    public IPage<SalesOrder> pageOrders(SalesOrderVo vo) {
        IPage<SalesOrder> page = new Page<>(vo.getPage(), vo.getLimit());
        QueryWrapper<SalesOrder> qw = new QueryWrapper<>();
        qw.like(StringUtils.isNotBlank(vo.getOrderNo()), "order_no", vo.getOrderNo());
        qw.like(StringUtils.isNotBlank(vo.getInvoiceNo()), "invoice_no", vo.getInvoiceNo());
        qw.eq(vo.getCustomerId() != null, "customer_id", vo.getCustomerId());
        qw.eq(vo.getWarehouseId() != null, "warehouse_id", vo.getWarehouseId());
        qw.eq(StringUtils.isNotBlank(vo.getStatus()), "status", vo.getStatus());
        qw.eq(StringUtils.isNotBlank(vo.getPaidStatus()), "paid_status", vo.getPaidStatus());
        qw.eq(StringUtils.isNotBlank(vo.getReceiveStatus()), "receive_status", vo.getReceiveStatus());
        qw.eq(StringUtils.isNotBlank(vo.getOrderType()), "order_type", vo.getOrderType());
        qw.eq(StringUtils.isNotBlank(vo.getOrderChannel()), "order_channel", vo.getOrderChannel());
        qw.eq(StringUtils.isNotBlank(vo.getPreprocessStatus()), "preprocess_status", vo.getPreprocessStatus());
        qw.like(StringUtils.isNotBlank(vo.getOriginalInvoiceNo()), "original_invoice_no", vo.getOriginalInvoiceNo());
        qw.orderByDesc("id");
        this.page(page, qw);
        page.getRecords().forEach(this::fillHeaderNames);
        return page;
    }

    @Override
    public SalesOrder getDetail(Long id) {
        SalesOrder order = this.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("出库单不存在");
        }
        fillHeaderNames(order);
        order.setItems(loadItems(id));
        voucherService.fillSales(order);
        if (PharmaNos.ORDER_NORMAL.equals(defaultType(order.getOrderType()))) {
            fillRemainingQty(order);
        }
        return order;
    }

    @Override
    public SalesOrder getByInvoiceNo(String invoiceNo) {
        SalesOrder order = findByInvoiceQuery(invoiceNo);
        if (order == null) {
            throw new IllegalArgumentException("未找到发票号：" + invoiceNo + "。可输入 20 位全电号码、后 10 位顺序号或出库单号");
        }
        return getDetail(order.getId());
    }

    @Override
    public SalesOrder findByInvoiceQuery(String invoiceNo) {
        if (StringUtils.isBlank(invoiceNo)) {
            return null;
        }
        String q = invoiceNo.trim();
        QueryWrapper<SalesOrder> qw = new QueryWrapper<>();
        qw.and(w -> w.eq("invoice_no", q)
                .or().eq("order_no", q)
                .or().eq("einvoice_no", q)
                .or().likeRight("invoice_no", q)
                .or().likeLeft("invoice_no", q));
        qw.isNotNull("invoice_no");
        qw.ne("invoice_no", "");
        qw.orderByDesc("id");
        qw.last("LIMIT 1");
        return this.getOne(qw, false);
    }

    @Override
    @Transactional
    public SalesOrder saveDraft(SalesOrderVo vo) {
        List<SalesOrderItem> items = vo.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("请至少添加一行明细");
        }
        if (vo.getCustomerId() == null) {
            throw new IllegalArgumentException("请选择客户");
        }
        if (vo.getWarehouseId() == null) {
            throw new IllegalArgumentException("请选择仓库");
        }
        if (vo.getBizDate() == null) {
            throw new IllegalArgumentException("请选择出库日期");
        }

        SalesOrder order;
        if (vo.getId() == null) {
            order = new SalesOrder();
            boolean reversal = PharmaNos.ORDER_REVERSAL.equals(vo.getOrderType());
            order.setOrderNo(nextOrderNo(reversal ? "HC" : "CK"));
            order.setStatus(PharmaNos.STATUS_DRAFT);
            order.setOrderType(reversal ? PharmaNos.ORDER_REVERSAL : PharmaNos.ORDER_NORMAL);
        } else {
            order = this.getById(vo.getId());
            if (order == null) {
                throw new IllegalArgumentException("出库单不存在");
            }
            assertDraft(order);
            QueryWrapper<SalesOrderItem> delQw = new QueryWrapper<>();
            delQw.eq("order_id", order.getId());
            itemService.remove(delQw);
        }

        String invoiceNo = resolveInvoiceNo(StringUtils.trimToNull(vo.getInvoiceNo()), vo.getBizDate(), order.getId());
        order.setInvoiceNo(invoiceNo);
        order.setCustomerId(vo.getCustomerId());
        order.setWarehouseId(vo.getWarehouseId());
        order.setSalesmanId(vo.getSalesmanId());
        order.setReviewerId(vo.getReviewerId());
        order.setBizDate(vo.getBizDate());
        order.setPayType(vo.getPayType());
        order.setShipTime(vo.getShipTime());
        order.setEinvoiceNo(StringUtils.trimToNull(vo.getEinvoiceNo()));
        order.setEinvoicePath(StringUtils.trimToNull(vo.getEinvoicePath()));
        order.setRemark(vo.getRemark());
        if (StringUtils.isNotBlank(vo.getOrderChannel())) {
            order.setOrderChannel(vo.getOrderChannel());
        } else if (StringUtils.isBlank(order.getOrderChannel())) {
            order.setOrderChannel("OFFLINE");
        }
        if (vo.getPlatformNo() != null) {
            order.setPlatformNo(StringUtils.trimToNull(vo.getPlatformNo()));
        }
        if (vo.getId() != null) {
            order.setPreprocessStatus("待预处理");
            order.setCreditOk(0);
            order.setLicenseOk(0);
            order.setAllocateOk(0);
            order.setPriceLocked(0);
            order.setPreprocessRemark("草稿已改，须重新预处理后再开票");
        } else if (StringUtils.isBlank(order.getPreprocessStatus())) {
            order.setPreprocessStatus("待预处理");
        }
        if (PharmaNos.ORDER_REVERSAL.equals(defaultType(order.getOrderType()))) {
            applyReversalDraft(order, vo, items);
        } else {
            order.setOriginalInvoiceNo(null);
            if (order.getId() == null && StringUtils.isBlank(order.getPaidStatus())) {
                order.setPaidStatus(PharmaNos.PAID_NONE);
                order.setPaidAmount(BigDecimal.ZERO);
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (SalesOrderItem item : items) {
            normalizeSalesItem(item);
            total = total.add(item.getAmount());
        }
        order.setTotalAmount(total);

        if (order.getId() == null) {
            this.save(order);
        } else {
            this.updateById(order);
        }

        for (SalesOrderItem item : items) {
            item.setId(null);
            item.setOrderId(order.getId());
            if (StringUtils.isBlank(item.getSpdid())) {
                item.setSpdid(PharmaNos.spdid());
            }
            itemService.save(item);
        }
        return getDetail(order.getId());
    }

    @Override
    @Transactional
    public void confirm(Long id) {
        confirm(id, null, null, null);
    }

    @Override
    @Transactional
    public void confirm(Long id, String shipTime, String einvoiceNo, String einvoicePath) {
        String orderLock = "lock:order:" + id;
        if (!redisBiz.tryLockWait(orderLock, Duration.ofSeconds(8), Duration.ofMillis(1500), Duration.ofMillis(50))) {
            throw new IllegalArgumentException("该出库单正在确认，请勿重复提交");
        }
        try {
            SalesOrder order = this.getById(id);
            if (order == null) {
                throw new IllegalArgumentException("出库单不存在");
            }
            assertDraft(order);
            List<SalesOrderItem> items = loadItems(id);
            if (items.isEmpty()) {
                throw new IllegalArgumentException("出库单没有明细，无法确认");
            }
            boolean reversal = PharmaNos.ORDER_REVERSAL.equals(defaultType(order.getOrderType()));
            if (!reversal) {
                assertShipReady(items);
            }
            fillHeaderNames(order);
            voucherService.assertOutboundConfirm(order, items, reversal, einvoicePath);
            String invoiceNo = order.getInvoiceNo();
            if (StringUtils.isBlank(invoiceNo)) {
                invoiceNo = nextInvoiceNo(order.getBizDate(), order.getId());
            }
            Date now = new Date();
            UpdateWrapper<SalesOrder> claim = new UpdateWrapper<>();
            claim.eq("id", id).eq("status", PharmaNos.STATUS_DRAFT)
                    .set("status", PharmaNos.STATUS_CONFIRMED)
                    .set("confirmed_at", now)
                    .set("confirmed_by", currentUserId())
                    .set("invoice_no", invoiceNo);
            if (!reversal) {
                String storedPath = persistEinvoicePath(StringUtils.defaultIfBlank(einvoicePath, order.getEinvoicePath()));
                String storedNo = StringUtils.defaultIfBlank(einvoiceNo, StringUtils.defaultIfBlank(order.getEinvoiceNo(), invoiceNo));
                claim.set("ship_time", parseShipTime(shipTime))
                        .set("einvoice_no", storedNo)
                        .set("einvoice_path", storedPath)
                        .set("receive_status", PharmaNos.RECEIVE_PENDING);
            }
            if (!this.update(claim)) {
                throw new IllegalArgumentException("该出库单已确认或正在处理，请刷新");
            }
            if (reversal) {
                assertReversalQty(order, items, order.getId());
            }
            for (SalesOrderItem item : items) {
                if (reversal) {
                    batchStockService.increase(
                            item.getDrugId(),
                            order.getWarehouseId(),
                            item.getBatchNo(),
                            item.getQualityStatus(),
                            item.getQty(),
                            null,
                            item.getExpireDate()
                    );
                } else {
                    batchStockService.decrease(
                            item.getDrugId(),
                            order.getWarehouseId(),
                            item.getBatchNo(),
                            item.getQualityStatus(),
                            item.getQty()
                    );
                }
            }
        } finally {
            redisBiz.unlock(orderLock);
        }
    }

    @Override
    public IPage<SalesOrder> pagePendingReceipt(SalesOrderVo vo) {
        IPage<SalesOrder> page = new Page<>(vo.getPage(), vo.getLimit());
        QueryWrapper<SalesOrder> qw = new QueryWrapper<>();
        qw.eq("status", PharmaNos.STATUS_CONFIRMED);
        qw.and(w -> w.eq("order_type", PharmaNos.ORDER_NORMAL).or().isNull("order_type").or().eq("order_type", ""));
        String receiveStatus = StringUtils.defaultIfBlank(vo.getReceiveStatus(), PharmaNos.RECEIVE_PENDING);
        qw.eq("receive_status", receiveStatus);
        qw.eq(vo.getCustomerId() != null, "customer_id", vo.getCustomerId());
        qw.like(StringUtils.isNotBlank(vo.getInvoiceNo()), "invoice_no", vo.getInvoiceNo());
        qw.like(StringUtils.isNotBlank(vo.getOrderNo()), "order_no", vo.getOrderNo());
        qw.orderByDesc("id");
        this.page(page, qw);
        page.getRecords().forEach(this::fillHeaderNames);
        return page;
    }

    @Override
    @Transactional
    public void confirmReceipt(SalesOrderVo vo) {
        if (vo.getId() == null) {
            throw new IllegalArgumentException("请选择收货单据");
        }
        SalesOrder order = this.getById(vo.getId());
        if (order == null) {
            throw new IllegalArgumentException("出库单不存在");
        }
        if (PharmaNos.ORDER_REVERSAL.equals(defaultType(order.getOrderType()))) {
            throw new IllegalArgumentException("红冲单不需要医院收货");
        }
        if (!PharmaNos.STATUS_CONFIRMED.equals(order.getStatus())) {
            throw new IllegalArgumentException("只有已发货单据可以确认收货");
        }
        if (!PharmaNos.RECEIVE_PENDING.equals(order.getReceiveStatus())) {
            throw new IllegalArgumentException("该单据不是待收货状态");
        }
        boolean reject = PharmaNos.RECEIVE_ACTION_REJECT.equals(vo.getReceiveAction());
        voucherService.assertReceiptSign(order, reject);
        List<SalesOrderItem> dbItems = loadItems(order.getId());
        if (dbItems.isEmpty()) {
            throw new IllegalArgumentException("出库单没有明细");
        }
        Map<Long, SalesOrderItem> submitted = new HashMap<>();
        if (vo.getItems() != null) {
            for (SalesOrderItem item : vo.getItems()) {
                if (item.getId() != null) {
                    submitted.put(item.getId(), item);
                }
            }
        }
        int full = 0;
        int zero = 0;
        Date now = new Date();
        for (SalesOrderItem db : dbItems) {
            BigDecimal shipped = db.getQty() == null ? BigDecimal.ZERO : db.getQty();
            BigDecimal received;
            String lineRemark = null;
            if (reject) {
                received = BigDecimal.ZERO;
            } else {
                SalesOrderItem in = submitted.get(db.getId());
                received = (in == null || in.getReceivedQty() == null) ? shipped : in.getReceivedQty();
                lineRemark = in == null ? null : in.getReceiveRemark();
            }
            if (received.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("实收数量不能为负");
            }
            if (received.compareTo(shipped) > 0) {
                throw new IllegalArgumentException("实收不能大于发货数量：批号 " + db.getBatchNo());
            }
            if (received.compareTo(shipped) == 0) {
                full++;
            }
            if (received.compareTo(BigDecimal.ZERO) == 0) {
                zero++;
            }
            UpdateWrapper<SalesOrderItem> itemUw = new UpdateWrapper<>();
            itemUw.eq("id", db.getId())
                    .set("received_qty", received)
                    .set("receive_remark", lineRemark)
                    .set("updated_at", now);
            itemService.update(itemUw);
        }
        String receiveStatus;
        if (reject || zero == dbItems.size()) {
            receiveStatus = PharmaNos.RECEIVE_REJECTED;
        } else if (full == dbItems.size()) {
            receiveStatus = PharmaNos.RECEIVE_SIGNED;
        } else {
            receiveStatus = PharmaNos.RECEIVE_PARTIAL;
        }
        UpdateWrapper<SalesOrder> uw = new UpdateWrapper<>();
        uw.eq("id", order.getId())
                .eq("receive_status", PharmaNos.RECEIVE_PENDING)
                .set("receive_status", receiveStatus)
                .set("received_at", now)
                .set("received_by", currentUserId())
                .set("receive_remark", vo.getReceiveRemark())
                .set("updated_at", now);
        if (!this.update(uw)) {
            throw new IllegalArgumentException("收货状态已变化，请刷新后重试");
        }
    }

    @Override
    @Transactional
    public void deleteDraft(Long id) {
        SalesOrder order = this.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("出库单不存在");
        }
        assertDraft(order);
        QueryWrapper<SalesOrderItem> delQw = new QueryWrapper<>();
        delQw.eq("order_id", id);
        itemService.remove(delQw);
        voucherService.removeByBiz(PharmaNos.BIZ_SALES, id);
        this.removeById(id);
    }

    @Override
    public IPage<SalesOrder> pageUnpaid(SalesOrderVo vo) {
        IPage<SalesOrder> page = new Page<>(vo.getPage(), vo.getLimit());
        QueryWrapper<SalesOrder> qw = new QueryWrapper<>();
        qw.eq("status", PharmaNos.STATUS_CONFIRMED);
        qw.and(w -> w.ne("paid_status", PharmaNos.PAID_DONE).or().isNull("paid_status"));
        qw.eq(vo.getCustomerId() != null, "customer_id", vo.getCustomerId());
        qw.like(StringUtils.isNotBlank(vo.getInvoiceNo()), "invoice_no", vo.getInvoiceNo());
        qw.isNotNull("invoice_no");
        qw.ne("invoice_no", "");
        qw.and(w -> w.eq("order_type", PharmaNos.ORDER_NORMAL).or().isNull("order_type").or().eq("order_type", ""));
        qw.orderByDesc("id");
        this.page(page, qw);
        page.getRecords().forEach(this::fillHeaderNames);
        return page;
    }

    @Override
    public java.util.List<com.sunlee.bus.vo.CustomerDebtVo> listCustomerDebt() {
        QueryWrapper<SalesOrder> qw = new QueryWrapper<>();
        qw.eq("status", PharmaNos.STATUS_CONFIRMED);
        qw.and(w -> w.eq("order_type", PharmaNos.ORDER_NORMAL).or().isNull("order_type").or().eq("order_type", ""));
        qw.isNotNull("invoice_no");
        qw.ne("invoice_no", "");
        java.util.Map<Long, com.sunlee.bus.vo.CustomerDebtVo> map = new java.util.LinkedHashMap<>();
        for (SalesOrder order : this.list(qw)) {
            Long cid = order.getCustomerId();
            if (cid == null) {
                continue;
            }
            fillHeaderNames(order);
            com.sunlee.bus.vo.CustomerDebtVo row = map.computeIfAbsent(cid, id -> {
                com.sunlee.bus.vo.CustomerDebtVo v = new com.sunlee.bus.vo.CustomerDebtVo();
                v.setCustomerId(id);
                v.setCustomerName(order.getCustomerName());
                v.setInvoiceCount(0);
                v.setTotalAmount(BigDecimal.ZERO);
                v.setPaidAmount(BigDecimal.ZERO);
                v.setUnpaidAmount(BigDecimal.ZERO);
                return v;
            });
            BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
            BigDecimal paid = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
            if (paid.compareTo(total) > 0) {
                paid = total;
            }
            row.setInvoiceCount(row.getInvoiceCount() + 1);
            row.setTotalAmount(row.getTotalAmount().add(total));
            row.setPaidAmount(row.getPaidAmount().add(paid));
            row.setUnpaidAmount(row.getUnpaidAmount().add(total.subtract(paid)));
        }
        return new java.util.ArrayList<>(map.values());
    }

    @Override
    public void markPaid(Long id, String paidStatus, BigDecimal paidAmount) {
        throw new IllegalArgumentException("回款只能由银行到账流水经冲账确认后产生，不能口头标记");
    }

    @Override
    @Transactional
    public SalesOrder saveReversal(SalesOrderVo vo) {
        if (StringUtils.isBlank(vo.getOriginalInvoiceNo())) {
            throw new IllegalArgumentException("请填写原发票号");
        }
        SalesOrder original = findOriginal(vo.getOriginalInvoiceNo());
        vo.setOrderType(PharmaNos.ORDER_REVERSAL);
        vo.setCustomerId(original.getCustomerId());
        vo.setWarehouseId(original.getWarehouseId());
        if (vo.getBizDate() == null) {
            vo.setBizDate(LocalDate.now());
        }
        return saveDraft(vo);
    }

    @Override
    public List<SalesOrder> listReversals(String originalInvoiceNo) {
        if (StringUtils.isBlank(originalInvoiceNo)) {
            return List.of();
        }
        QueryWrapper<SalesOrder> qw = new QueryWrapper<>();
        qw.eq("original_invoice_no", originalInvoiceNo.trim());
        qw.eq("order_type", PharmaNos.ORDER_REVERSAL);
        qw.orderByDesc("id");
        List<SalesOrder> list = this.list(qw);
        for (SalesOrder order : list) {
            fillHeaderNames(order);
            order.setItems(loadItems(order.getId()));
        }
        return list;
    }

    private void applyReversalDraft(SalesOrder order, SalesOrderVo vo, List<SalesOrderItem> items) {
        SalesOrder original = findOriginal(vo.getOriginalInvoiceNo());
        order.setOriginalInvoiceNo(original.getInvoiceNo());
        order.setCustomerId(original.getCustomerId());
        order.setWarehouseId(original.getWarehouseId());
        order.setPaidStatus(PharmaNos.PAID_DONE);
        order.setPaidAmount(BigDecimal.ZERO);
        assertReversalQty(original, items, order.getId());
    }

    private SalesOrder findOriginal(String invoiceNo) {
        if (StringUtils.isBlank(invoiceNo)) {
            throw new IllegalArgumentException("红冲必须指定原发票号");
        }
        QueryWrapper<SalesOrder> qw = new QueryWrapper<>();
        qw.eq("invoice_no", invoiceNo.trim());
        qw.last("LIMIT 1");
        SalesOrder original = this.getOne(qw, false);
        if (original == null) {
            throw new IllegalArgumentException("未找到原发票号：" + invoiceNo);
        }
        if (!PharmaNos.STATUS_CONFIRMED.equals(original.getStatus())) {
            throw new IllegalArgumentException("只能红冲已确认的出库发票");
        }
        if (PharmaNos.ORDER_REVERSAL.equals(defaultType(original.getOrderType()))) {
            throw new IllegalArgumentException("不能对红冲单再开红冲");
        }
        return original;
    }

    private void assertReversalQty(SalesOrder originalOrSelf, List<SalesOrderItem> items, Long excludeOrderId) {
        String originalNo = originalOrSelf.getInvoiceNo();
        if (PharmaNos.ORDER_REVERSAL.equals(defaultType(originalOrSelf.getOrderType()))) {
            originalNo = originalOrSelf.getOriginalInvoiceNo();
        }
        SalesOrder original = findOriginal(originalNo);
        List<SalesOrderItem> originItems = loadItems(original.getId());
        for (SalesOrderItem item : items) {
            SalesOrderItem originLine = originItems.stream()
                    .filter(it -> it.getDrugId().equals(item.getDrugId())
                            && StringUtils.equals(it.getBatchNo(), item.getBatchNo()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "红冲明细必须来自原发票同一药品和批号"));
            if (item.getExpireDate() == null) {
                item.setExpireDate(originLine.getExpireDate());
            }
            if (item.getSalePrice() == null) {
                item.setSalePrice(originLine.getSalePrice());
            }
            BigDecimal remain = remainQty(original.getInvoiceNo(), originLine, excludeOrderId);
            if (item.getQty().compareTo(remain) > 0) {
                throw new IllegalArgumentException("红冲数量超过可冲数量：批号 "
                        + item.getBatchNo() + " 最多还可冲 " + remain);
            }
        }
    }

    private void fillRemainingQty(SalesOrder order) {
        if (order.getItems() == null) {
            return;
        }
        for (SalesOrderItem item : order.getItems()) {
            item.setRemainingQty(remainQty(order.getInvoiceNo(), item, null));
        }
    }

    private BigDecimal remainQty(String originalInvoiceNo, SalesOrderItem originLine, Long excludeOrderId) {
        BigDecimal used = reversedQty(originalInvoiceNo, originLine.getDrugId(), originLine.getBatchNo(), excludeOrderId);
        BigDecimal originQty = originLine.getQty() == null ? BigDecimal.ZERO : originLine.getQty();
        BigDecimal remain = originQty.subtract(used);
        return remain.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remain;
    }

    private BigDecimal reversedQty(String originalInvoiceNo, Long drugId, String batchNo, Long excludeOrderId) {
        QueryWrapper<SalesOrder> qw = new QueryWrapper<>();
        qw.eq("original_invoice_no", originalInvoiceNo);
        qw.eq("order_type", PharmaNos.ORDER_REVERSAL);
        if (excludeOrderId != null) {
            qw.ne("id", excludeOrderId);
        }
        List<SalesOrder> reversals = this.list(qw);
        BigDecimal sum = BigDecimal.ZERO;
        for (SalesOrder reversal : reversals) {
            for (SalesOrderItem item : loadItems(reversal.getId())) {
                if (drugId.equals(item.getDrugId()) && StringUtils.equals(batchNo, item.getBatchNo())) {
                    sum = sum.add(item.getQty() == null ? BigDecimal.ZERO : item.getQty());
                }
            }
        }
        return sum;
    }

    private String defaultType(String orderType) {
        return StringUtils.isBlank(orderType) ? PharmaNos.ORDER_NORMAL : orderType;
    }

    private void assertDraft(SalesOrder order) {
        if (!PharmaNos.STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("只有草稿单据可以修改或删除");
        }
    }

    private String resolveInvoiceNo(String invoiceNo, LocalDate bizDate, Long excludeId) {
        if (invoiceNo == null) {
            return nextInvoiceNo(bizDate, excludeId);
        }
        PharmaNos.assertInvoiceNo(invoiceNo, bizDate);
        if (invoiceExists(invoiceNo, excludeId)) {
            throw new IllegalArgumentException("发票号已存在：" + invoiceNo);
        }
        return invoiceNo;
    }

    private String nextInvoiceNo(LocalDate bizDate, Long excludeId) {
        long redisSeq = redisBiz.nextInvoiceSeq(bizDate);
        if (redisSeq > 0) {
            if (redisSeq < PharmaNos.INVOICE_SEQ_START) {
                redisSeq = PharmaNos.INVOICE_SEQ_START + redisSeq - 1;
            }
            String candidate = PharmaNos.invoiceNo(bizDate, redisSeq);
            while (invoiceExists(candidate, excludeId)) {
                redisSeq++;
                candidate = PharmaNos.invoiceNo(bizDate, redisSeq);
            }
            return candidate;
        }
        String prefix = PharmaNos.invoiceCode(bizDate);
        QueryWrapper<SalesOrder> qw = new QueryWrapper<>();
        qw.select("MAX(invoice_no) AS invoice_no");
        qw.likeRight("invoice_no", prefix);
        if (excludeId != null) {
            qw.ne("id", excludeId);
        }
        SalesOrder last = this.getOne(qw, false);
        long seq = PharmaNos.INVOICE_SEQ_START;
        if (last != null && StringUtils.length(last.getInvoiceNo()) == PharmaNos.INVOICE_LEN
                && last.getInvoiceNo().startsWith(prefix)) {
            seq = Math.max(PharmaNos.parseSeq(last.getInvoiceNo()) + 1, PharmaNos.INVOICE_SEQ_START);
        }
        String candidate = PharmaNos.invoiceNo(bizDate, seq);
        while (invoiceExists(candidate, excludeId)) {
            seq++;
            candidate = PharmaNos.invoiceNo(bizDate, seq);
        }
        return candidate;
    }

    private boolean invoiceExists(String invoiceNo, Long excludeId) {
        QueryWrapper<SalesOrder> qw = new QueryWrapper<>();
        qw.eq("invoice_no", invoiceNo);
        if (excludeId != null) {
            qw.ne("id", excludeId);
        }
        return this.count(qw) > 0;
    }

    private String nextOrderNo(String prefix) {
        for (int i = 0; i < 5; i++) {
            String no = PharmaNos.orderNo(prefix);
            QueryWrapper<SalesOrder> qw = new QueryWrapper<>();
            qw.eq("order_no", no);
            if (this.count(qw) == 0) {
                return no;
            }
        }
        return PharmaNos.orderNo(prefix) + Thread.currentThread().getId();
    }

    private void normalizeSalesItem(SalesOrderItem item) {
        if (item.getDrugId() == null) {
            throw new IllegalArgumentException("明细必须选择药品");
        }
        if (StringUtils.isBlank(item.getBatchNo())) {
            throw new IllegalArgumentException("明细必须填写批号");
        }
        if (item.getQty() == null || item.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("明细数量必须大于 0");
        }
        if (item.getQualityStatus() == null || item.getQualityStatus().isBlank()) {
            item.setQualityStatus(PharmaNos.QUALITY_OK);
        }
        BigDecimal price = item.getSalePrice() == null ? BigDecimal.ZERO : item.getSalePrice();
        item.setSalePrice(price);
        item.setAmount(PharmaAmounts.lineAmount(item.getQty(), price));
    }

    private List<SalesOrderItem> loadItems(Long orderId) {
        QueryWrapper<SalesOrderItem> qw = new QueryWrapper<>();
        qw.eq("order_id", orderId);
        qw.orderByAsc("id");
        List<SalesOrderItem> items = itemService.list(qw);
        for (SalesOrderItem item : items) {
            if (item.getDrugId() != null) {
                Drug drug = drugService.getById(item.getDrugId());
                if (drug != null) {
                    item.setDrugName(drug.getGenericName());
                    item.setDrugSpec(drug.getSpec());
                }
            }
        }
        return items;
    }

    private void fillHeaderNames(SalesOrder order) {
        if (order.getCustomerId() != null) {
            Customer customer = customerService.getById(order.getCustomerId());
            if (customer != null) {
                order.setCustomerName(customer.getName());
            }
        }
        if (order.getWarehouseId() != null) {
            Warehouse warehouse = warehouseService.getById(order.getWarehouseId());
            if (warehouse != null) {
                order.setWarehouseName(warehouse.getName());
            }
        }
    }

    private void assertShipReady(List<SalesOrderItem> items) {
        for (SalesOrderItem item : items) {
            if (StringUtils.isBlank(item.getBatchNo())) {
                throw new IllegalArgumentException("发货前每行必须填写批号");
            }
            if (item.getQty() == null || item.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("发货前每行数量必须大于 0");
            }
            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("发货前每行必须有金额");
            }
        }
    }

    private Date parseShipTime(String shipTime) {
        if (StringUtils.isBlank(shipTime)) {
            return new Date();
        }
        String text = shipTime.trim().replace('T', ' ');
        if (text.length() == 16) {
            text = text + ":00";
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            throw new IllegalArgumentException("发货时间格式应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    @Override
    @Transactional
    public SalesOrder simulateEinvoice(Long id) {
        SalesOrder order = getDetail(id);
        if (!PharmaNos.STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("只有草稿出库单可以模拟开票；已确认单发票已锁定");
        }
        if (PharmaNos.ORDER_REVERSAL.equals(order.getOrderType())) {
            throw new IllegalArgumentException("红冲单不单独开具正数发票");
        }
        if (!"已通过".equals(order.getPreprocessStatus())) {
            throw new IllegalArgumentException("须先通过订单预处理（信誉额、证照、分货、价格）才能开票");
        }
        String invoiceNo = order.getInvoiceNo();
        if (StringUtils.isBlank(invoiceNo)) {
            invoiceNo = nextInvoiceNo(order.getBizDate(), order.getId());
            order.setInvoiceNo(invoiceNo);
        }
        fillHeaderNames(order);
        StringBuilder rows = new StringBuilder();
        for (SalesOrderItem item : order.getItems()) {
            rows.append("<tr><td>").append(com.sunlee.bus.common.SimInvoiceDocs.escape(item.getDrugName()))
                    .append("</td><td>").append(com.sunlee.bus.common.SimInvoiceDocs.escape(item.getBatchNo()))
                    .append("</td><td>").append(item.getQty())
                    .append("</td><td>").append(item.getSalePrice())
                    .append("</td></tr>");
        }
        String html = "<h1>电子发票（模拟·我方开具）</h1>"
                + "<p>销货方：药衡医药</p>"
                + "<p>购货方：" + com.sunlee.bus.common.SimInvoiceDocs.escape(order.getCustomerName()) + "</p>"
                + "<p>全电发票号码：" + com.sunlee.bus.common.SimInvoiceDocs.escape(invoiceNo) + "</p>"
                + "<p>开票日期：" + order.getBizDate() + "</p>"
                + "<table><tr><th>品种</th><th>批号</th><th>数量</th><th>单价</th></tr>"
                + rows + "</table>"
                + "<p>价税合计：" + order.getTotalAmount() + "</p>";
        String path = com.sunlee.bus.common.SimInvoiceDocs.writeHtml("einvoice.html", "电子发票", html);
        order.setEinvoiceNo(invoiceNo);
        order.setEinvoicePath(path);
        this.updateById(order);
        voucherService.replaceGeneratedFile(PharmaNos.BIZ_SALES, order.getId(), "invoice",
                "电子发票-" + invoiceNo + ".html", html);
        return getDetail(id);
    }

    @Override
    @Transactional
    public SalesOrder preprocess(Long id) {
        SalesOrder order = getDetail(id);
        assertDraft(order);
        Customer customer = customerService.getById(order.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        boolean license = StringUtils.isNotBlank(customer.getLicenseNo()) && (customer.getStatus() == null || customer.getStatus() == 1);
        QueryWrapper<OpsFlowDoc> creditQw = new QueryWrapper<>();
        creditQw.eq("doc_type", "CREDIT").eq("customer_id", order.getCustomerId()).eq("status", "已生效").orderByDesc("id").last("LIMIT 1");
        OpsFlowDoc credit = opsFlowDocMapper.selectOne(creditQw);
        BigDecimal limit = credit == null || credit.getAmount() == null ? BigDecimal.ZERO : credit.getAmount();
        QueryWrapper<SalesOrder> unpaidQw = new QueryWrapper<>();
        unpaidQw.eq("customer_id", order.getCustomerId()).eq("status", PharmaNos.STATUS_CONFIRMED)
                .ne("paid_status", PharmaNos.PAID_DONE);
        BigDecimal occupied = BigDecimal.ZERO;
        for (SalesOrder row : this.list(unpaidQw)) {
            BigDecimal amt = row.getTotalAmount() == null ? BigDecimal.ZERO : row.getTotalAmount();
            BigDecimal paid = row.getPaidAmount() == null ? BigDecimal.ZERO : row.getPaidAmount();
            occupied = occupied.add(amt.subtract(paid));
        }
        BigDecimal need = occupied.add(order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount());
        boolean creditOk = credit != null && limit.compareTo(need) >= 0;
        QueryWrapper<OpsFlowDoc> allocQw = new QueryWrapper<>();
        allocQw.eq("doc_type", "ALLOCATE").eq("status", "待分货").eq("customer_id", order.getCustomerId());
        boolean allocateOk = opsFlowDocMapper.selectCount(allocQw) == 0;
        boolean priceOk = true;
        for (SalesOrderItem item : order.getItems()) {
            if (item.getSalePrice() == null || item.getSalePrice().compareTo(BigDecimal.ZERO) <= 0) {
                priceOk = false;
                break;
            }
        }
        java.util.List<String> issues = new java.util.ArrayList<>();
        if (!creditOk) {
            issues.add(credit == null ? "无已生效信誉额" : "欠款加本单超过授信 " + limit);
        }
        if (!license) {
            issues.add("客户证照缺失或已停用");
        }
        if (!allocateOk) {
            issues.add("该客户仍有待分货单，分货完成才能开票");
        }
        if (!priceOk) {
            issues.add("成交价未锁定（明细单价须大于0）");
        }
        order.setCreditOk(creditOk ? 1 : 0);
        order.setLicenseOk(license ? 1 : 0);
        order.setAllocateOk(allocateOk ? 1 : 0);
        order.setPriceLocked(priceOk ? 1 : 0);
        if (issues.isEmpty()) {
            order.setPreprocessStatus("已通过");
            order.setPreprocessRemark("信誉额、证照、分货、价格均通过，可开票");
        } else {
            order.setPreprocessStatus("未通过");
            order.setPreprocessRemark(String.join("；", issues));
        }
        this.updateById(order);
        return getDetail(id);
    }

    @Override
    @Transactional
    public SalesOrder importPlatformOrder() {
        QueryWrapper<Customer> cq = new QueryWrapper<>();
        cq.eq("status", 1).last("LIMIT 1");
        Customer customer = customerService.getOne(cq, false);
        if (customer == null) {
            throw new IllegalArgumentException("没有启用客户，无法接入网单");
        }
        QueryWrapper<Warehouse> wq = new QueryWrapper<>();
        wq.last("LIMIT 1");
        Warehouse warehouse = warehouseService.getOne(wq, false);
        if (warehouse == null) {
            throw new IllegalArgumentException("没有仓库");
        }
        QueryWrapper<BatchStock> bq = new QueryWrapper<>();
        bq.gt("qty", 0).eq("quality_status", PharmaNos.QUALITY_OK).orderByDesc("id").last("LIMIT 1");
        BatchStock stock = batchStockService.getOne(bq, false);
        if (stock == null) {
            throw new IllegalArgumentException("没有合格库存，网单不能跳过预处理直接发货");
        }
        Drug drug = drugService.getById(stock.getDrugId());
        SalesOrderVo vo = new SalesOrderVo();
        vo.setCustomerId(customer.getId());
        vo.setWarehouseId(stock.getWarehouseId() != null ? stock.getWarehouseId() : warehouse.getId());
        vo.setBizDate(LocalDate.now());
        vo.setPayType(StringUtils.defaultIfBlank(customer.getSettleType(), "月结"));
        vo.setOrderChannel("PLATFORM");
        vo.setPlatformNo("WD" + LocalDate.now().toString().replace("-", "") + String.format("%04d", (int) (Math.random() * 9999)));
        vo.setRemark("全药网/药交平台网单接入，须预处理后才能开票");
        SalesOrderItem item = new SalesOrderItem();
        item.setDrugId(stock.getDrugId());
        item.setBatchNo(stock.getBatchNo());
        item.setExpireDate(stock.getExpireDate());
        item.setQty(BigDecimal.ONE);
        item.setSalePrice(drug != null && drug.getRefSalePrice() != null ? drug.getRefSalePrice() : BigDecimal.ONE);
        item.setQualityStatus(PharmaNos.QUALITY_OK);
        vo.setItems(java.util.List.of(item));
        return saveDraft(vo);
    }

    private String persistEinvoicePath(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        return AppFileUtils.renameFile(path);
    }

    private Long currentUserId() {
        try {
            User user = (User) StpUtil.getSession().get("user");
            return user == null ? null : user.getId().longValue();
        } catch (Exception e) {
            return null;
        }
    }
}
