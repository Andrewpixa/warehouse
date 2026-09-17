package com.sunlee.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * InnoDB free: 9216 kB
 * </p>
 *
 * @author sunlee
 * @since 2026-02-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_dept")
@ToString
public class Dept implements Serializable {

    private static final long serialVersionUID=1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer pid;

    /** 部门名称 dept_name */
    private String name;

    private String deptCode;

    /** 公司 / 总经办 / 销售 / 仓储 / 质量 / 采购 / 财务 / 信息 / 其他 */
    private String deptType;

    private Integer managerUserId;

    private String phone;

    /** 逗号分隔：首营审核,收货验收,... */
    private String gspRoles;

    /**
     * 是否展开，0不展开，1展开
     */
    private Integer open;

    private String remark;

    private String address;

    /**
     * 是否可用，0不可用，1可用
     */
    private Integer available;

    /**
     * 排序码
     */
    private Integer ordernum;

    private Date createtime;

    @TableField(exist = false)
    private String parentName;

    @TableField(exist = false)
    private String managerName;

    @TableField(exist = false)
    private Integer memberCount;
}
