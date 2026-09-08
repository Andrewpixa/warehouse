package com.sunlee.bus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 药品档案（pharma_ims.drugs）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("drugs")
public class Drug implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private String code;

    /** 药品 / 器械，对应编号前缀 3 / 4 */
    private String category;

    private String genericName;

    private String tradeName;

    private String spec;

    private String dosageForm;

    private String unit;

    private String manufacturer;

    private String approvalNo;

    private String barcode;

    private Integer isColdChain;

    private String rxType;

    private BigDecimal refPurchasePrice;

    private BigDecimal refSalePrice;

    /** 1启用 0停用 */
    private Integer status;

    private String remark;

    private Date createdAt;

    private Date updatedAt;
}
