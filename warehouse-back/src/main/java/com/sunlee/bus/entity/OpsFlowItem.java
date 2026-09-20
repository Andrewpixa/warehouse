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
@TableName("ops_flow_items")
public class OpsFlowItem implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long docId;

    /** 冲账勾兑发票号 */
    private String relatedNo;

    private Long drugId;

    private String batchNo;

    private BigDecimal qty;

    private BigDecimal price;

    private BigDecimal amount;

    private String qualityStatus;

    private LocalDate expireDate;

    private String remark;

    private Date createdAt;

    @TableField(exist = false)
    private String drugName;
}
