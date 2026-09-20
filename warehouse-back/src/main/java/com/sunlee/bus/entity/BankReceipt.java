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
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("bank_receipts")
public class BankReceipt implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String receiptNo;

    private Long customerId;

    private LocalDate receivedDate;

    private BigDecimal amount;

    /** 公对公 / 支票 */
    private String channel;

    private String voucherNo;

    private String payerName;

    /** 未认领 / 部分认领 / 已认领 / 挂账 */
    private String status;

    private String remark;

    private Long createdBy;

    private Date createdAt;

    private Date updatedAt;

    @TableField(exist = false)
    private String customerName;

    @TableField(exist = false)
    private BigDecimal allocatedAmount;

    @TableField(exist = false)
    private BigDecimal remainAmount;
}
