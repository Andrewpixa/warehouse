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
 * 出库单明细（pharma_ims.sales_order_items）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sales_order_items")
public class SalesOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long drugId;

    private String batchNo;

    private LocalDate expireDate;

    private BigDecimal qty;

    /** 医院实收数量 */
    private BigDecimal receivedQty;

    private String receiveRemark;

    private BigDecimal salePrice;

    private BigDecimal amount;

    private String spdid;

    private String qualityStatus;

    private String remark;

    private Date createdAt;

    private Date updatedAt;

    @TableField(exist = false)
    private String drugName;

    @TableField(exist = false)
    private String drugSpec;

    @TableField(exist = false)
    private BigDecimal availableQty;

    /** 原出发票该行还可红冲的数量 */
    @TableField(exist = false)
    private BigDecimal remainingQty;
}
