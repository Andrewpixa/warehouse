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
 * 批号升益单主表（pharma_ims.surplus_orders）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("surplus_orders")
public class SurplusOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long warehouseId;

    private LocalDate bizDate;

    /** 草稿 / 已确认 */
    private String status;

    private BigDecimal totalSurplusQty;

    private BigDecimal totalSurplusAmount;

    private BigDecimal totalLossQty;

    private BigDecimal totalLossAmount;

    private Long createdBy;

    private Long confirmedBy;

    private Date confirmedAt;

    private String remark;

    private Date createdAt;

    private Date updatedAt;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private List<SurplusOrderItem> items = new ArrayList<>();
}
