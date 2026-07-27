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
 * 分仓库存（一期数量按仓粒度管理；bus_goods.number 为各仓之和的总库存缓存）
 *
 * @author sunlee
 * @since 2026-07-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("bus_goods_stock")
public class GoodsStock implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 商品ID */
    private Integer goodsid;

    /** 仓库ID */
    private Integer warehouseId;

    /** 该仓库存数量 */
    private Integer number;

    /** 商品名称（非数据库字段） */
    @TableField(exist = false)
    private String goodsname;

    /** 商品规格（非数据库字段） */
    @TableField(exist = false)
    private String size;

    /** 仓库名称（非数据库字段） */
    @TableField(exist = false)
    private String warehouseName;

    /** 预警阈值（非数据库字段，分仓预警查询时填充） */
    @TableField(exist = false)
    private Integer dangernum;

}
