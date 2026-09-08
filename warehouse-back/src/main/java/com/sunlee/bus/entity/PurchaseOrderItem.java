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
import java.util.Date;

/**
 * 进货单明细（pharma_ims.purchase_order_items）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("purchase_order_items")
public class PurchaseOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long drugId;

    private String batchNo;

    private LocalDate productionDate;

    private LocalDate expireDate;

    private BigDecimal receiveQty;

    private BigDecimal qualifiedQty;

    private BigDecimal stockInQty;

    private BigDecimal purchasePrice;

    private BigDecimal amount;

    private String qualityStatus;

    private String spdid;

    private String remark;

    private Date createdAt;

    private Date updatedAt;

    @TableField(exist = false)
    private String drugName;

    @TableField(exist = false)
    private String drugSpec;
}
