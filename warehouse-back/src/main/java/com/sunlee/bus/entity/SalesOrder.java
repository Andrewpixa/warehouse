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
 * 出库单主表（pharma_ims.sales_orders）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sales_orders")
public class SalesOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private String invoiceNo;

    /** 正常 / 红冲 */
    private String orderType;

    /** 红冲单指向的原蓝字发票号 */
    private String originalInvoiceNo;

    private Long customerId;

    private Long warehouseId;

    private Long salesmanId;

    private Long reviewerId;

    private LocalDate bizDate;

    private String payType;

    /** 草稿 / 已确认 / 已过账 */
    private String status;

    private BigDecimal totalAmount;

    /** 未回款 / 部分回款 / 已回款 */
    private String paidStatus;

    private BigDecimal paidAmount;

    private Date paidAt;

    private Long createdBy;

    private Long confirmedBy;

    private Date confirmedAt;

    /** 发货时间（确认出库时填写） */
    private Date shipTime;

    /** 电子发票号，默认等于发票号 */
    private String einvoiceNo;

    /** 电子发票文件路径 */
    private String einvoicePath;

    /** 待收货 / 已签收 / 部分签收 / 拒收 */
    private String receiveStatus;

    private Date receivedAt;

    private Long receivedBy;

    private String receiveRemark;

    private String remark;

    private Date createdAt;

    private Date updatedAt;

    /** PLATFORM 网单 / OFFLINE 开票员录入 BMS */
    private String orderChannel;

    private String platformNo;

    /** 待预处理 / 已通过 / 未通过 */
    private String preprocessStatus;

    private Integer creditOk;

    private Integer licenseOk;

    private Integer allocateOk;

    private Integer priceLocked;

    private String preprocessRemark;

    @TableField(exist = false)
    private String customerName;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private List<SalesOrderItem> items = new ArrayList<>();

    @TableField(exist = false)
    private List<BizAttachment> attachments = new ArrayList<>();

    @TableField(exist = false)
    private List<BizSignature> signatures = new ArrayList<>();
}
