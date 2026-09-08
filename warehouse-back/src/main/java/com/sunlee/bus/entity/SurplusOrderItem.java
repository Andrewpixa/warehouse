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
 * 批号升益单明细（pharma_ims.surplus_order_items）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("surplus_order_items")
public class SurplusOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long drugId;

    private String batchNo;

    private String qualityStatus;

    private BigDecimal bookQty;

    private BigDecimal actualQty;

    private BigDecimal surplusQty;

    private BigDecimal lossQty;

    private BigDecimal unitCost;

    private BigDecimal surplusAmount;

    private BigDecimal lossAmount;

    private LocalDate productionDate;

    private LocalDate expireDate;

    private String remark;

    private Date createdAt;

    private Date updatedAt;

    @TableField(exist = false)
    private String drugName;

    @TableField(exist = false)
    private String drugSpec;
}
