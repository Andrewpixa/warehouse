package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.common.PharmaNos;
import com.sunlee.bus.entity.Drug;
import com.sunlee.bus.entity.PurchaseOrder;
import com.sunlee.bus.entity.PurchaseOrderItem;
import com.sunlee.bus.entity.Supplier;
import com.sunlee.bus.entity.Warehouse;
import com.sunlee.bus.mapper.PurchaseOrderMapper;
import com.sunlee.bus.common.PharmaAmounts;
import com.sunlee.bus.service.IBatchStockService;
import com.sunlee.bus.service.IBizVoucherService;
import com.sunlee.bus.service.IDrugService;
import com.sunlee.bus.service.IPurchaseOrderItemService;
import com.sunlee.bus.service.IPurchaseOrderService;
import com.sunlee.bus.service.ISupplierService;
import com.sunlee.bus.service.IWarehouseService;
import com.sunlee.bus.vo.PurchaseOrderVo;
import com.sunlee.sys.entity.User;
import cn.dev33.satoken.stp.StpUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder>
        implements IPurchaseOrderService {

    @Autowired
    private IPurchaseOrderItemService itemService;

    @Autowired
    private IBatchStockService batchStockService;

    @Autowired
    private ISupplierService supplierService;

    @Autowired
    private IWarehouseService warehouseService;

    @Autowired
    private IDrugService drugService;

    @Autowired
    private IBizVoucherService voucherService;

    @Override
    public IPage<PurchaseOrder> pageOrders(PurchaseOrderVo vo) {
        IPage<PurchaseOrder> page = new Page<>(vo.getPage(), vo.getLimit());
        QueryWrapper<PurchaseOrder> qw = new QueryWrapper<>();
        qw.like(StringUtils.isNotBlank(vo.getOrderNo()), "order_no", vo.getOrderNo());
        qw.eq(vo.getSupplierId() != null, "supplier_id", vo.getSupplierId());
        qw.eq(vo.getWarehouseId() != null, "warehouse_id", vo.getWarehouseId());
        qw.eq(StringUtils.isNotBlank(vo.getStatus()), "status", vo.getStatus());
        qw.orderByDesc("id");
        this.page(page, qw);
        page.getRecords().forEach(this::fillHeaderNames);
        return page;
    }

    @Override
    public PurchaseOrder getDetail(Long id) {
        PurchaseOrder order = this.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("进货单不存在");
        }
        fillHeaderNames(order);
        order.setItems(loadItems(id));
        voucherService.fillPurchase(order);
        return order;
    }

    @Override
    @Transactional
    public PurchaseOrder saveDraft(PurchaseOrderVo vo) {
        List<PurchaseOrderItem> items = vo.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("请至少添加一行明细");
        }
        if (vo.getSupplierId() == null) {
            throw new IllegalArgumentException("请选择供应商");
        }
        if (vo.getWarehouseId() == null) {
            throw new IllegalArgumentException("请选择仓库");
        }
        if (vo.getBizDate() == null) {
            throw new IllegalArgumentException("请选择进货日期");
        }

        PurchaseOrder order;
        if (vo.getId() == null) {
            order = new PurchaseOrder();
            order.setOrderNo(nextOrderNo());
            order.setStatus(PharmaNos.STATUS_DRAFT);
        } else {
            order = this.getById(vo.getId());
            if (order == null) {
                throw new IllegalArgumentException("进货单不存在");
            }
            assertDraft(order);
            QueryWrapper<PurchaseOrderItem> delQw = new QueryWrapper<>();
            delQw.eq("order_id", order.getId());
            itemService.remove(delQw);
        }

        order.setSupplierId(vo.getSupplierId());
        order.setWarehouseId(vo.getWarehouseId());
        order.setSalesmanId(vo.getSalesmanId());
        order.setCheckerId(vo.getCheckerId());
        order.setKeeperId(vo.getKeeperId());
        order.setBizDate(vo.getBizDate());
        order.setCheckResult(vo.getCheckResult());
        if (StringUtils.isBlank(vo.getInvoiceNo())) {
            order.setInvoiceNo(previewSupplierInvoiceNo(vo.getSupplierId(), vo.getBizDate()));
        } else {
            order.setInvoiceNo(StringUtils.trimToNull(vo.getInvoiceNo()));
        }
        order.setRemark(vo.getRemark());

        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem item : items) {
            normalizePurchaseItem(item);
            total = total.add(item.getAmount());
        }
        order.setTotalAmount(total);

        if (order.getId() == null) {
            this.save(order);
        } else {
            this.updateById(order);
        }

        for (PurchaseOrderItem item : items) {
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
        PurchaseOrder order = this.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("进货单不存在");
        }
        assertDraft(order);
        if (!PharmaNos.QUALITY_OK.equals(order.getCheckResult())) {
            throw new IllegalArgumentException("请先将验收结论填写为合格，才能确认入库");
        }
        List<PurchaseOrderItem> items = loadItems(id);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("进货单没有明细，无法确认");
        }
        fillHeaderNames(order);
        voucherService.assertPurchaseConfirm(order, items);
        for (PurchaseOrderItem item : items) {
            if (!PharmaNos.QUALITY_OK.equals(item.getQualityStatus())) {
                throw new IllegalArgumentException("明细存在待验或不合格品种，不能确认入库");
            }
            BigDecimal qty = firstPositive(item.getStockInQty(), item.getQualifiedQty(), item.getReceiveQty());
            if (qty == null) {
                throw new IllegalArgumentException("明细入库数量必须大于 0");
            }
            batchStockService.increase(
                    item.getDrugId(),
                    order.getWarehouseId(),
                    item.getBatchNo(),
                    item.getQualityStatus(),
                    qty,
                    item.getProductionDate(),
                    item.getExpireDate()
            );
        }
        User user = currentUser();
        order.setStatus(PharmaNos.STATUS_CONFIRMED);
        order.setConfirmedBy(user == null || user.getId() == null ? null : user.getId().longValue());
        order.setConfirmedAt(new Date());
        this.updateById(order);
    }

    @Override
    @Transactional
    public void deleteDraft(Long id) {
        PurchaseOrder order = this.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("进货单不存在");
        }
        assertDraft(order);
        QueryWrapper<PurchaseOrderItem> delQw = new QueryWrapper<>();
        delQw.eq("order_id", id);
        itemService.remove(delQw);
        voucherService.removeByBiz(PharmaNos.BIZ_PURCHASE, id);
        this.removeById(id);
    }

    @Override
    public String previewSupplierInvoiceNo(Long supplierId, java.time.LocalDate bizDate) {
        if (supplierId == null) {
            throw new IllegalArgumentException("请先选择供应商");
        }
        java.time.LocalDate d = bizDate == null ? java.time.LocalDate.now() : bizDate;
        String prefix = com.sunlee.bus.common.SimInvoiceDocs.supplierInvoiceNo(supplierId, d, 0);
        prefix = prefix.substring(0, prefix.length() - 4);
        QueryWrapper<PurchaseOrder> qw = new QueryWrapper<>();
        qw.likeRight("invoice_no", prefix);
        int seq = (int) this.count(qw) + 1;
        String no = com.sunlee.bus.common.SimInvoiceDocs.supplierInvoiceNo(supplierId, d, seq);
        while (this.count(new QueryWrapper<PurchaseOrder>().eq("invoice_no", no)) > 0) {
            seq++;
            no = com.sunlee.bus.common.SimInvoiceDocs.supplierInvoiceNo(supplierId, d, seq);
        }
        return no;
    }

    @Override
    @Transactional
    public PurchaseOrder receiveSupplierInvoice(Long id) {
        PurchaseOrder order = getDetail(id);
        assertDraft(order);
        if (order.getSupplierId() == null) {
            throw new IllegalArgumentException("请先选择供应商");
        }
        fillHeaderNames(order);
        if (StringUtils.isBlank(order.getInvoiceNo())) {
            order.setInvoiceNo(previewSupplierInvoiceNo(order.getSupplierId(), order.getBizDate()));
            this.updateById(order);
        }
        String supplier = order.getSupplierName() == null ? "供应商" : order.getSupplierName();
        StringBuilder rows = new StringBuilder();
        for (PurchaseOrderItem item : order.getItems()) {
            rows.append("<tr><td>").append(com.sunlee.bus.common.SimInvoiceDocs.escape(item.getDrugName()))
                    .append("</td><td>").append(com.sunlee.bus.common.SimInvoiceDocs.escape(item.getBatchNo()))
                    .append("</td><td>").append(item.getStockInQty())
                    .append("</td><td>").append(item.getPurchasePrice())
                    .append("</td></tr>");
        }
        String invoiceHtml = "<h1>增值税普通发票（模拟·供方开具）</h1>"
                + "<p>销货方：" + com.sunlee.bus.common.SimInvoiceDocs.escape(supplier) + "</p>"
                + "<p>发票号码：" + com.sunlee.bus.common.SimInvoiceDocs.escape(order.getInvoiceNo()) + "</p>"
                + "<p>开票日期：" + order.getBizDate() + "</p>"
                + "<p>购货方：药衡医药</p>"
                + "<table><tr><th>品种</th><th>批号</th><th>数量</th><th>单价</th></tr>"
                + rows + "</table>"
                + "<p>价税合计：" + order.getTotalAmount() + "</p>";
        voucherService.replaceGeneratedFile(PharmaNos.BIZ_PURCHASE, order.getId(), "invoice",
                "供应商发票-" + order.getInvoiceNo() + ".html", invoiceHtml);
        String packHtml = "<h1>随货同行单（模拟·供方随货）</h1>"
                + "<p>供货单位：" + com.sunlee.bus.common.SimInvoiceDocs.escape(supplier) + "</p>"
                + "<p>对应发票：" + com.sunlee.bus.common.SimInvoiceDocs.escape(order.getInvoiceNo()) + "</p>"
                + "<p>收货仓库：" + com.sunlee.bus.common.SimInvoiceDocs.escape(order.getWarehouseName()) + "</p>"
                + "<table><tr><th>品种</th><th>批号</th><th>数量</th><th>单价</th></tr>"
                + rows + "</table>";
        voucherService.replaceGeneratedFile(PharmaNos.BIZ_PURCHASE, order.getId(), "packing",
                "随货同行-" + order.getOrderNo() + ".html", packHtml);
        return getDetail(id);
    }

    private void assertDraft(PurchaseOrder order) {
        if (!PharmaNos.STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("只有草稿单据可以修改或删除");
        }
    }

    private User currentUser() {
        try {
            return (User) StpUtil.getSession().get("user");
        } catch (Exception e) {
            return null;
        }
    }

    private String nextOrderNo() {
        for (int i = 0; i < 5; i++) {
            String no = PharmaNos.orderNo("CG");
            QueryWrapper<PurchaseOrder> qw = new QueryWrapper<>();
            qw.eq("order_no", no);
            if (this.count(qw) == 0) {
                return no;
            }
        }
        return PharmaNos.orderNo("CG") + Thread.currentThread().getId();
    }

    private void normalizePurchaseItem(PurchaseOrderItem item) {
        if (item.getDrugId() == null) {
            throw new IllegalArgumentException("明细必须选择药品");
        }
        if (StringUtils.isBlank(item.getBatchNo())) {
            throw new IllegalArgumentException("明细必须填写批号");
        }
        if (item.getQualityStatus() == null || item.getQualityStatus().isBlank()) {
            item.setQualityStatus(PharmaNos.QUALITY_OK);
        }
        BigDecimal qty = firstPositive(item.getStockInQty(), item.getQualifiedQty(), item.getReceiveQty());
        if (qty == null) {
            throw new IllegalArgumentException("明细收货/合格/入库数量必须大于 0");
        }
        if (item.getReceiveQty() == null) {
            item.setReceiveQty(qty);
        }
        if (item.getQualifiedQty() == null) {
            item.setQualifiedQty(qty);
        }
        if (item.getStockInQty() == null) {
            item.setStockInQty(qty);
        }
        BigDecimal price = item.getPurchasePrice() == null ? BigDecimal.ZERO : item.getPurchasePrice();
        item.setPurchasePrice(price);
        item.setAmount(PharmaAmounts.lineAmount(qty, price));
    }

    private BigDecimal firstPositive(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                return value;
            }
        }
        return null;
    }

    private List<PurchaseOrderItem> loadItems(Long orderId) {
        QueryWrapper<PurchaseOrderItem> qw = new QueryWrapper<>();
        qw.eq("order_id", orderId);
        qw.orderByAsc("id");
        List<PurchaseOrderItem> items = itemService.list(qw);
        for (PurchaseOrderItem item : items) {
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

    private void fillHeaderNames(PurchaseOrder order) {
        if (order.getSupplierId() != null) {
            Supplier supplier = supplierService.getById(order.getSupplierId());
            if (supplier != null) {
                order.setSupplierName(supplier.getName());
            }
        }
        if (order.getWarehouseId() != null) {
            Warehouse warehouse = warehouseService.getById(order.getWarehouseId());
            if (warehouse != null) {
                order.setWarehouseName(warehouse.getName());
            }
        }
    }
}
