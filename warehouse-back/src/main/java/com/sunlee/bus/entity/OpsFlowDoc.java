package com.sunlee.bus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
@TableName("ops_flow_docs")
public class OpsFlowDoc implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** STOCKOUT / INBOUND_EX / RETURN_NOTICE / OFFSET / ALLOCATE / LOGISTICS / CREDIT / QUOTA / PRINT_PACK / FLOW */
    private String docType;

    private String docNo;

    private LocalDate bizDate;

    private String status;

    private Long customerId;

    private Long supplierId;

    private Long drugId;

    private Long warehouseId;

    private String relatedNo;

    /** 银行到账流水，冲账单必填 */
    private Long receiptId;

    /** 1=金额不一致挂账，确认后不改发票回款 */
    private Integer hangFlag;

    private BigDecimal amount;

    private BigDecimal qty;

    private String title;

    private String reason;

    private String extraJson;

    private Long createdBy;

    private Long confirmedBy;

    private Date confirmedAt;

    private String remark;

    private Date createdAt;

    private Date updatedAt;

    @TableField(exist = false)
    private String customerName;

    @TableField(exist = false)
    private String supplierName;

    @TableField(exist = false)
    private String drugName;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private List<OpsFlowItem> items = new ArrayList<>();
}
