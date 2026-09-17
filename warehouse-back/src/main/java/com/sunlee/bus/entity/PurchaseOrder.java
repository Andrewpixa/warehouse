package com.sunlee.bus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 进货单主表（pharma_ims.purchase_orders）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("purchase_orders")
public class PurchaseOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNo;

    /** 供应商发票号 */
    private String invoiceNo;

    private Long supplierId;

    private Long warehouseId;

    private Long salesmanId;

    private Long checkerId;

    private Long keeperId;

    private LocalDate bizDate;

    private String checkResult;

    /** 草稿 / 已确认 / 已过账 */
    private String status;

    private BigDecimal totalAmount;

    private Long createdBy;

    private Long confirmedBy;

    private Date confirmedAt;

    private String remark;

    private Date createdAt;

    private Date updatedAt;

    @TableField(exist = false)
    private String supplierName;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @TableField(exist = false)
    private List<BizAttachment> attachments = new ArrayList<>();

    @TableField(exist = false)
    private List<BizSignature> signatures = new ArrayList<>();
}
