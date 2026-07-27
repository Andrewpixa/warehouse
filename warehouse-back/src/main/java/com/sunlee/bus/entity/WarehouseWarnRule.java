package com.sunlee.bus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 分仓预警规则（有规则才做分仓预警；无规则的商品仍走商品级总量预警）
 *
 * @author sunlee
 * @since 2026-07-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("bus_warehouse_warn_rule")
public class WarehouseWarnRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 商品ID */
    private Integer goodsid;

    /** 仓库ID */
    private Integer warehouseId;

    /** 该仓预警阈值 */
    private Integer dangernum;

    /** 商品名称（非数据库字段） */
    @TableField(exist = false)
    private String goodsname;

    /** 仓库名称（非数据库字段） */
    @TableField(exist = false)
    private String warehouseName;

}
