package com.sunlee.bus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("maker_flow_accounts")
public class MakerFlowAccount implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String loginName;

    private String password;

    private Long supplierId;

    private Integer status;

    private Date effectiveAt;

    private String remark;

    private Date createdAt;

    @TableField(exist = false)
    private String supplierName;
}
