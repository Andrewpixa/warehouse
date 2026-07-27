package com.sunlee.bus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 仓库
 *
 * @author sunlee
 * @since 2026-07-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("bus_warehouse")
public class Warehouse implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 仓库名称 */
    private String name;

    /** 仓库编码 */
    private String code;

    /** 地址 */
    private String address;

    /** 负责人 */
    private String manager;

    /** 联系电话 */
    private String phone;

    /** 是否默认仓 0=否 1=是（全系统唯一） */
    private Integer isDefault;

    /** 状态 0=停用 1=启用 */
    private Integer available;

    private String remark;

    private Date createTime;

}
