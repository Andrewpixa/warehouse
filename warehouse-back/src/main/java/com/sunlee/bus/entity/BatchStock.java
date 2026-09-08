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
 * 批号库存（pharma_ims.batch_stocks）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("batch_stocks")
public class BatchStock implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long drugId;

    private Long warehouseId;

    private String batchNo;

    private LocalDate productionDate;

    private LocalDate expireDate;

    private BigDecimal qty;

    private String qualityStatus;

    private Date lastMoveAt;

    private Date createdAt;

    private Date updatedAt;

    @TableField(exist = false)
    private String drugName;

    @TableField(exist = false)
    private String drugSpec;

    @TableField(exist = false)
    private String warehouseName;
}
