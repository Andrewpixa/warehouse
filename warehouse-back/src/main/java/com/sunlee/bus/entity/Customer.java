package com.sunlee.bus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 客户档案（pharma_ims.customers）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("customers")
@ToString
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private String code;

    private String name;

    private String customerType;

    private String licenseNo;

    private String contact;

    private String phone;

    private String address;

    private String settleType;

    /** 1启用 0停用 */
    private Integer status;

    private String remark;

    private Date createdAt;

    private Date updatedAt;
}
