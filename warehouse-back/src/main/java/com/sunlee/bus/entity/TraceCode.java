package com.sunlee.bus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 追溯码（pharma_ims.trace_codes）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("trace_codes")
public class TraceCode implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String spdid;

    private String code;

    private String packLevel;

    /** 上级包装码，最小包装指中包装，中包装指大包装 */
    private String parentCode;

    private String bizType;

    /** 正常 / 作废 */
    private String status;

    private Date collectedAt;

    private String remark;

    private Date createdAt;

    private Date updatedAt;

    @TableField(exist = false)
    private String invoiceNo;

    @TableField(exist = false)
    private String orderNo;

    @TableField(exist = false)
    private String drugName;

    @TableField(exist = false)
    private String batchNo;
}
