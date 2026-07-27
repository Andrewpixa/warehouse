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
 * 调拨单明细
 *
 * @author sunlee
 * @since 2026-07-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("bus_transfer_item")
public class TransferItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 调拨单ID */
    private Integer transferId;

    /** 商品ID */
    private Integer goodsid;

    /** 调拨数量 */
    private Integer number;

    /** 调出库位ID（拣货指引） */
    private Integer fromLocationId;

    /** 调入库位ID */
    private Integer toLocationId;

    /** 商品名称（非数据库字段） */
    @TableField(exist = false)
    private String goodsname;

    /** 商品规格（非数据库字段） */
    @TableField(exist = false)
    private String size;

}
