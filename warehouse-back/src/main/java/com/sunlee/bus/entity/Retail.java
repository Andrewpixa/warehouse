package com.sunlee.bus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("bus_retail")
public class Retail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String orderno;

    private Integer orderStatus;

    private Integer goodsid;

    /** 仓库ID（出库仓） */
    private Integer warehouseId;

    /** 库位ID（拣货指引） */
    private Integer locationId;

    private String paytype;

    private Date retailtime;

    private String operateperson;

    private Integer number;

    private String remark;

    private Double retailprice;

    @TableLogic
    private Integer isdelete;

    @TableField(exist = false)
    private String goodsname;

    @TableField(exist = false)
    private String size;

    /**
     * 仓库名称
     */
    @TableField(exist = false)
    private String warehouseName;

}
