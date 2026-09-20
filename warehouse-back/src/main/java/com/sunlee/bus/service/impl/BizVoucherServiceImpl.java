package com.sunlee.bus.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sunlee.bus.common.PharmaAmounts;
import com.sunlee.bus.common.PharmaNos;
import com.sunlee.bus.entity.BizAttachment;
import com.sunlee.bus.entity.BizSignature;
import com.sunlee.bus.entity.PurchaseOrder;
import com.sunlee.bus.entity.PurchaseOrderItem;
import com.sunlee.bus.entity.SalesOrder;
import com.sunlee.bus.entity.SalesOrderItem;
import com.sunlee.bus.mapper.BizAttachmentMapper;
import com.sunlee.bus.mapper.BizSignatureMapper;
import com.sunlee.bus.service.IBizVoucherService;
import com.sunlee.bus.vo.VoucherIssue;
import com.sunlee.sys.common.AppFileUtils;
import com.sunlee.sys.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class BizVoucherServiceImpl implements IBizVoucherService {

    public static final String BIZ_PURCHASE = "purchase";
    public static final String BIZ_SALES = "sales";
    public static final String ATTACH_INVOICE = "invoice";
    public static final String ATTACH_PACKING = "packing";
    public static final String ATTACH_CONTRACT = "contract";
    public static final String SIGN_PURCHASE_CHECK = "purchase_check";
    public static final String SIGN_PURCHASE_KEEP = "purchase_keep";
    public static final String SIGN_DELIVERY = "delivery";
    public static final String SIGN_CUSTOMER = "customer";
    public static final String SOURCE_PAD = "pad";
    public static final String SOURCE_PHOTO = "photo";

    @Autowired
    private BizAttachmentMapper attachmentMapper;

    @Autowired
    private BizSignatureMapper signatureMapper;

    @Override
    public List<BizAttachment> listAttachments(String bizType, Long bizId) {
        QueryWrapper<BizAttachment> qw = new QueryWrapper<>();
        qw.eq("biz_type", bizType).eq("biz_id", bizId).orderByAsc("id");
        return attachmentMapper.selectList(qw);
    }

    @Override
    @Transactional
    public BizAttachment saveAttachment(String bizType, Long bizId, String attachType, String filePath, String fileName) {
        assertBiz(bizType, bizId);
        if (!ATTACH_INVOICE.equals(attachType) && !ATTACH_PACKING.equals(attachType)
                && !ATTACH_CONTRACT.equals(attachType)) {
            throw new IllegalArgumentException("附件类型只能是采购合同、发票或随货同行单");
        }
        if (StringUtils.isBlank(filePath)) {
            throw new IllegalArgumentException("请先上传文件");
        }
        Date now = new Date();
        BizAttachment row = new BizAttachment();
        row.setBizType(bizType);
        row.setBizId(bizId);
        row.setAttachType(attachType);
        row.setFilePath(persistPath(filePath));
        row.setFileName(StringUtils.trimToNull(fileName));
        row.setCreatedBy(currentUserId());
        row.setCreatedAt(now);
        attachmentMapper.insert(row);
        return row;
    }

    @Override
    @Transactional
    public void deleteAttachment(Long id) {
        BizAttachment row = attachmentMapper.selectById(id);
        if (row == null) {
            throw new IllegalArgumentException("附件不存在");
        }
        attachmentMapper.deleteById(id);
    }

    @Override
    public List<BizSignature> listSignatures(String bizType, Long bizId) {
        QueryWrapper<BizSignature> qw = new QueryWrapper<>();
        qw.eq("biz_type", bizType).eq("biz_id", bizId).orderByAsc("id");
        return signatureMapper.selectList(qw);
    }

    @Override
    @Transactional
    public BizSignature saveSignature(String bizType, Long bizId, String signRole, String signerName,
                                      String signSource, String imagePath) {
        assertBiz(bizType, bizId);
        assertSignRole(signRole);
        if (StringUtils.isBlank(signerName)) {
            throw new IllegalArgumentException("请填写签字人姓名");
        }
        if (!SOURCE_PAD.equals(signSource) && !SOURCE_PHOTO.equals(signSource)) {
            throw new IllegalArgumentException("签字来源只能是手写板或拍照");
        }
        if (StringUtils.isBlank(imagePath)) {
            throw new IllegalArgumentException("请完成签字或上传签字照片");
        }
        Date now = new Date();
        QueryWrapper<BizSignature> qw = new QueryWrapper<>();
        qw.eq("biz_type", bizType).eq("biz_id", bizId).eq("sign_role", signRole);
        BizSignature row = signatureMapper.selectOne(qw);
        if (row == null) {
            row = new BizSignature();
            row.setBizType(bizType);
            row.setBizId(bizId);
            row.setSignRole(signRole);
            row.setCreatedBy(currentUserId());
            row.setCreatedAt(now);
        }
        row.setSignerName(signerName.trim());
        row.setSignSource(signSource);
        row.setImagePath(persistPath(imagePath));
        row.setSignedAt(now);
        if (row.getId() == null) {
            signatureMapper.insert(row);
        } else {
            signatureMapper.updateById(row);
        }
        return row;
    }

    @Override
    @Transactional
    public void deleteSignature(Long id) {
        BizSignature row = signatureMapper.selectById(id);
        if (row == null) {
            throw new IllegalArgumentException("签字不存在");
        }
        signatureMapper.deleteById(id);
    }

    @Override
    public void fillPurchase(PurchaseOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        order.setAttachments(listAttachments(BIZ_PURCHASE, order.getId()));
        order.setSignatures(listSignatures(BIZ_PURCHASE, order.getId()));
    }

    @Override
    public void fillSales(SalesOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        order.setAttachments(listAttachments(BIZ_SALES, order.getId()));
        order.setSignatures(listSignatures(BIZ_SALES, order.getId()));
    }

    @Override
    @Transactional
    public void removeByBiz(String bizType, Long bizId) {
        QueryWrapper<BizAttachment> aqw = new QueryWrapper<>();
        aqw.eq("biz_type", bizType).eq("biz_id", bizId);
        attachmentMapper.delete(aqw);
        QueryWrapper<BizSignature> sqw = new QueryWrapper<>();
        sqw.eq("biz_type", bizType).eq("biz_id", bizId);
        signatureMapper.delete(sqw);
    }

    @Override
    public void assertPurchaseConfirm(PurchaseOrder order, List<PurchaseOrderItem> items) {
        List<VoucherIssue> issues = inspectPurchase(order, items, order.getSupplierName());
        throwFirst(issues);
    }

    @Override
    public void assertOutboundConfirm(SalesOrder order, List<SalesOrderItem> items, boolean reversal, String einvoicePath) {
        List<VoucherIssue> issues = inspectSalesInternal(order, items, order.getCustomerName(), reversal, einvoicePath, false);
        throwFirst(issues);
    }

    @Override
    public void assertReceiptSign(SalesOrder order, boolean reject) {
        if (reject) {
            return;
        }
        if (!hasSign(BIZ_SALES, order.getId(), SIGN_CUSTOMER)) {
            throw new IllegalArgumentException("签收前请采集客户签字（手写板或拍照）");
        }
    }

    @Override
    public List<VoucherIssue> inspectPurchase(PurchaseOrder order, List<PurchaseOrderItem> items, String partnerName) {
        List<VoucherIssue> issues = new ArrayList<>();
        Long id = order.getId();
        String no = order.getOrderNo();
        String invoice = order.getInvoiceNo();
        if (StringUtils.isBlank(invoice)) {
            issues.add(issue(BIZ_PURCHASE, id, no, invoice, partnerName, VoucherIssue.MISSING_TICKET,
                    "缺少供应商发票号", "/business/purchase"));
        }
        if (!hasAttach(BIZ_PURCHASE, id, ATTACH_CONTRACT)) {
            issues.add(issue(BIZ_PURCHASE, id, no, invoice, partnerName, VoucherIssue.MISSING_TICKET,
                    "缺少采购合同", "/business/purchase"));
        }
        if (!hasAttach(BIZ_PURCHASE, id, ATTACH_INVOICE)) {
            issues.add(issue(BIZ_PURCHASE, id, no, invoice, partnerName, VoucherIssue.MISSING_TICKET,
                    "缺少发票照片/PDF", "/business/purchase"));
        }
        if (!hasAttach(BIZ_PURCHASE, id, ATTACH_PACKING)) {
            issues.add(issue(BIZ_PURCHASE, id, no, invoice, partnerName, VoucherIssue.MISSING_TICKET,
                    "缺少随货同行单", "/business/purchase"));
        }
        if (!hasSign(BIZ_PURCHASE, id, SIGN_PURCHASE_CHECK)) {
            issues.add(issue(BIZ_PURCHASE, id, no, invoice, partnerName, VoucherIssue.MISSING_SIGN,
                    "缺少进货验收签字", "/business/purchase"));
        }
        if (!hasSign(BIZ_PURCHASE, id, SIGN_PURCHASE_KEEP)) {
            issues.add(issue(BIZ_PURCHASE, id, no, invoice, partnerName, VoucherIssue.MISSING_SIGN,
                    "缺少到货保管签字", "/business/purchase"));
        }
        issues.addAll(inspectPurchaseAmounts(order, items, partnerName));
        return issues;
    }

    @Override
    public List<VoucherIssue> inspectSales(SalesOrder order, List<SalesOrderItem> items, String partnerName) {
        boolean reversal = PharmaNos.ORDER_REVERSAL.equals(order.getOrderType());
        return inspectSalesInternal(order, items, partnerName, reversal, order.getEinvoicePath(), true);
    }

    private List<VoucherIssue> inspectSalesInternal(SalesOrder order, List<SalesOrderItem> items, String partnerName,
                                                    boolean reversal, String einvoicePath, boolean inspectReceiptSign) {
        List<VoucherIssue> issues = new ArrayList<>();
        Long id = order.getId();
        String no = order.getOrderNo();
        String invoice = order.getInvoiceNo();
        if (StringUtils.isBlank(invoice)) {
            issues.add(issue(BIZ_SALES, id, no, invoice, partnerName, VoucherIssue.MISSING_TICKET,
                    "缺少发票号", "/business/outbound"));
        }
        if (!reversal) {
            boolean hasInvoiceFile = hasAttach(BIZ_SALES, id, ATTACH_INVOICE) || StringUtils.isNotBlank(einvoicePath);
            if (!hasInvoiceFile) {
                issues.add(issue(BIZ_SALES, id, no, invoice, partnerName, VoucherIssue.MISSING_TICKET,
                        "缺少电子发票/发票影像", "/business/outbound"));
            }
            if (!hasAttach(BIZ_SALES, id, ATTACH_PACKING)) {
                issues.add(issue(BIZ_SALES, id, no, invoice, partnerName, VoucherIssue.MISSING_TICKET,
                        "缺少随货同行单", "/business/outbound"));
            }
            if (!hasSign(BIZ_SALES, id, SIGN_DELIVERY)) {
                issues.add(issue(BIZ_SALES, id, no, invoice, partnerName, VoucherIssue.MISSING_SIGN,
                        "缺少送货签字", "/business/outbound"));
            }
        }
        if (inspectReceiptSign && !reversal && isSigned(order) && !hasSign(BIZ_SALES, id, SIGN_CUSTOMER)) {
            issues.add(issue(BIZ_SALES, id, no, invoice, partnerName, VoucherIssue.MISSING_SIGN,
                    "已签收但缺少客户签字", "/business/receipt"));
        }
        issues.addAll(inspectSalesAmounts(order, items, partnerName));
        return issues;
    }

    private List<VoucherIssue> inspectPurchaseAmounts(PurchaseOrder order, List<PurchaseOrderItem> items, String partnerName) {
        List<VoucherIssue> issues = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (PurchaseOrderItem item : items) {
                if (StringUtils.isBlank(item.getBatchNo())) {
                    issues.add(issue(BIZ_PURCHASE, order.getId(), order.getOrderNo(), order.getInvoiceNo(), partnerName,
                            VoucherIssue.AMOUNT_MISMATCH, "明细缺少批号", "/business/purchase"));
                }
                BigDecimal qty = firstPositive(item.getStockInQty(), item.getQualifiedQty(), item.getReceiveQty());
                BigDecimal expected = PharmaAmounts.lineAmount(qty, item.getPurchasePrice());
                sum = sum.add(expected);
                if (qty == null) {
                    issues.add(issue(BIZ_PURCHASE, order.getId(), order.getOrderNo(), order.getInvoiceNo(), partnerName,
                            VoucherIssue.AMOUNT_MISMATCH, "明细数量必须大于 0", "/business/purchase"));
                } else if (!PharmaAmounts.moneyEquals(expected, item.getAmount())) {
                    issues.add(issue(BIZ_PURCHASE, order.getId(), order.getOrderNo(), order.getInvoiceNo(), partnerName,
                            VoucherIssue.AMOUNT_MISMATCH,
                            "批号 " + item.getBatchNo() + " 金额应为数量×单价 " + expected.toPlainString(),
                            "/business/purchase"));
                }
            }
        }
        if (!PharmaAmounts.moneyEquals(sum, order.getTotalAmount())) {
            issues.add(issue(BIZ_PURCHASE, order.getId(), order.getOrderNo(), order.getInvoiceNo(), partnerName,
                    VoucherIssue.AMOUNT_MISMATCH,
                    "表头金额 " + PharmaAmounts.nz(order.getTotalAmount()).toPlainString()
                            + " 与明细合计 " + sum.toPlainString() + " 不一致",
                    "/business/purchase"));
        }
        return issues;
    }

    private List<VoucherIssue> inspectSalesAmounts(SalesOrder order, List<SalesOrderItem> items, String partnerName) {
        List<VoucherIssue> issues = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (SalesOrderItem item : items) {
                if (StringUtils.isBlank(item.getBatchNo())) {
                    issues.add(issue(BIZ_SALES, order.getId(), order.getOrderNo(), order.getInvoiceNo(), partnerName,
                            VoucherIssue.AMOUNT_MISMATCH, "明细缺少批号", "/business/outbound"));
                }
                BigDecimal expected = PharmaAmounts.lineAmount(item.getQty(), item.getSalePrice());
                sum = sum.add(expected);
                if (item.getQty() == null || item.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                    issues.add(issue(BIZ_SALES, order.getId(), order.getOrderNo(), order.getInvoiceNo(), partnerName,
                            VoucherIssue.AMOUNT_MISMATCH, "明细数量必须大于 0", "/business/outbound"));
                } else if (!PharmaAmounts.moneyEquals(expected, item.getAmount())) {
                    issues.add(issue(BIZ_SALES, order.getId(), order.getOrderNo(), order.getInvoiceNo(), partnerName,
                            VoucherIssue.AMOUNT_MISMATCH,
                            "批号 " + item.getBatchNo() + " 金额应为数量×单价 " + expected.toPlainString(),
                            "/business/outbound"));
                }
            }
        }
        if (!PharmaAmounts.moneyEquals(sum, order.getTotalAmount())) {
            issues.add(issue(BIZ_SALES, order.getId(), order.getOrderNo(), order.getInvoiceNo(), partnerName,
                    VoucherIssue.AMOUNT_MISMATCH,
                    "表头金额 " + PharmaAmounts.nz(order.getTotalAmount()).toPlainString()
                            + " 与明细合计 " + sum.toPlainString() + " 不一致",
                    "/business/outbound"));
        }
        return issues;
    }

    private boolean hasAttach(String bizType, Long bizId, String attachType) {
        QueryWrapper<BizAttachment> qw = new QueryWrapper<>();
        qw.eq("biz_type", bizType).eq("biz_id", bizId).eq("attach_type", attachType);
        return attachmentMapper.selectCount(qw) > 0;
    }

    private boolean hasSign(String bizType, Long bizId, String signRole) {
        QueryWrapper<BizSignature> qw = new QueryWrapper<>();
        qw.eq("biz_type", bizType).eq("biz_id", bizId).eq("sign_role", signRole);
        return signatureMapper.selectCount(qw) > 0;
    }

    private boolean isSigned(SalesOrder order) {
        String status = order.getReceiveStatus();
        return PharmaNos.RECEIVE_SIGNED.equals(status) || PharmaNos.RECEIVE_PARTIAL.equals(status);
    }

    private void throwFirst(List<VoucherIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return;
        }
        throw new IllegalArgumentException(issues.get(0).getMessage());
    }

    private VoucherIssue issue(String bizType, Long bizId, String orderNo, String invoiceNo,
                               String partnerName, String type, String message, String href) {
        return VoucherIssue.of(bizType, bizId, orderNo, invoiceNo, partnerName, type, message, href);
    }

    private void assertBiz(String bizType, Long bizId) {
        if (!BIZ_PURCHASE.equals(bizType) && !BIZ_SALES.equals(bizType)) {
            throw new IllegalArgumentException("业务类型无效");
        }
        if (bizId == null) {
            throw new IllegalArgumentException("请先保存草稿再上传票据或签字");
        }
    }

    private void assertSignRole(String signRole) {
        if (!SIGN_PURCHASE_CHECK.equals(signRole)
                && !SIGN_PURCHASE_KEEP.equals(signRole)
                && !SIGN_DELIVERY.equals(signRole)
                && !SIGN_CUSTOMER.equals(signRole)) {
            throw new IllegalArgumentException("签字角色无效");
        }
    }

    private String persistPath(String path) {
        if (StringUtils.isBlank(path)) {
            return path;
        }
        return AppFileUtils.renameFile(path);
    }

    @Override
    @Transactional
    public BizAttachment replaceGeneratedFile(String bizType, Long bizId, String attachType, String fileName, String htmlBody) {
        assertBiz(bizType, bizId);
        QueryWrapper<BizAttachment> qw = new QueryWrapper<>();
        qw.eq("biz_type", bizType).eq("biz_id", bizId).eq("attach_type", attachType);
        List<BizAttachment> old = attachmentMapper.selectList(qw);
        for (BizAttachment row : old) {
            AppFileUtils.removeFileByPath(row.getFilePath());
            attachmentMapper.deleteById(row.getId());
        }
        String path = com.sunlee.bus.common.SimInvoiceDocs.writeHtml(
                fileName == null ? "ticket.html" : fileName,
                attachType,
                htmlBody);
        return saveAttachment(bizType, bizId, attachType, path, fileName);
    }

    private Long currentUserId() {
        try {
            User user = (User) StpUtil.getSession().get("user");
            return user == null || user.getId() == null ? null : user.getId().longValue();
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal firstPositive(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                return value;
            }
        }
        return null;
    }
}
