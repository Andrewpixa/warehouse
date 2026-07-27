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
 * 库位（两级结构：仓库→库位，zone 字段做库区分组）
 *
 * @author sunlee
 * @since 2026-07-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("bus_warehouse_location")
public class WarehouseLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 所属仓库ID */
    private Integer warehouseId;

    /** 库位编码 如 A-01-03 */
    private String code;

    /** 库区（拣货区/存储区/退货区/不良品区） */
    private String zone;

    /** 库位名称 */
    private String name;

    /** 状态 0=停用 1=启用 */
    private Integer available;

    private String remark;

    /** 仓库名称（非数据库字段） */
    @TableField(exist = false)
    private String warehouseName;

}
