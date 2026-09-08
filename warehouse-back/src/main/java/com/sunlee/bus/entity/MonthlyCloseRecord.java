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
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("monthly_close_records")
public class MonthlyCloseRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** yyyy-MM */
    @TableField("close_month")
    private String yearMonth;

    private String status;

    private Integer customerCount;

    private BigDecimal salesAmount;

    private BigDecimal unpaidAmount;

    private Integer supplierCount;

    private BigDecimal purchaseAmount;

    private Long closedBy;

    private String closedByName;

    private Date closedAt;

    private String remark;

    private Date createdAt;

    private Date updatedAt;
}
