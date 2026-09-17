package com.sunlee.bus.service;

import com.sunlee.bus.entity.BizAttachment;
import com.sunlee.bus.entity.BizSignature;
import com.sunlee.bus.entity.PurchaseOrder;
import com.sunlee.bus.entity.PurchaseOrderItem;
import com.sunlee.bus.entity.SalesOrder;
import com.sunlee.bus.entity.SalesOrderItem;
import com.sunlee.bus.vo.VoucherIssue;

import java.util.List;

public interface IBizVoucherService {

    List<BizAttachment> listAttachments(String bizType, Long bizId);

    BizAttachment saveAttachment(String bizType, Long bizId, String attachType, String filePath, String fileName);

    void deleteAttachment(Long id);

    List<BizSignature> listSignatures(String bizType, Long bizId);

    BizSignature saveSignature(String bizType, Long bizId, String signRole, String signerName,
                               String signSource, String imagePath);

    void deleteSignature(Long id);

    void fillPurchase(PurchaseOrder order);

    void fillSales(SalesOrder order);

    void removeByBiz(String bizType, Long bizId);

    void assertPurchaseConfirm(PurchaseOrder order, List<PurchaseOrderItem> items);

    void assertOutboundConfirm(SalesOrder order, List<SalesOrderItem> items, boolean reversal, String einvoicePath);

    void assertReceiptSign(SalesOrder order, boolean reject);

    List<VoucherIssue> inspectPurchase(PurchaseOrder order, List<PurchaseOrderItem> items, String partnerName);

    List<VoucherIssue> inspectSales(SalesOrder order, List<SalesOrderItem> items, String partnerName);

    BizAttachment replaceGeneratedFile(String bizType, Long bizId, String attachType, String fileName, String htmlBody);
}
