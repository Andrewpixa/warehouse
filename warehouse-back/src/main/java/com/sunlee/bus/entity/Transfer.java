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
import java.util.List;

/**
 * 库存调拨单
 *
 * @author sunlee
 * @since 2026-07-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("bus_transfer")
public class Transfer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 调拨单号 */
    private String transferNo;

    /** 调出仓库ID */
    private Integer fromWarehouseId;

    /** 调入仓库ID */
    private Integer toWarehouseId;

    /** 状态 0=草稿 1=在途 2=已完成 3=已取消 */
    private Integer status;

    /** 操作人 */
    private String operator;

    private String remark;

    private Date createTime;

    /** 发出时间 */
    private Date shipTime;

    /** 完成时间 */
    private Date finishTime;

    /** 调出仓库名称（非数据库字段） */
    @TableField(exist = false)
    private String fromWarehouseName;

    /** 调入仓库名称（非数据库字段） */
    @TableField(exist = false)
    private String toWarehouseName;

    /** 调拨明细（非数据库字段） */
    @TableField(exist = false)
    private List<TransferItem> items;

}
