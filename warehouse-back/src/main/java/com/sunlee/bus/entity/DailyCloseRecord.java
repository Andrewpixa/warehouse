package com.sunlee.bus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("daily_close_records")
public class DailyCloseRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private LocalDate bizDate;

    private String status;

    private Integer purchaseCount;

    private BigDecimal purchaseAmount;

    private Integer outboundCount;

    private BigDecimal outboundAmount;

    private Long closedBy;

    private String closedByName;

    private Date closedAt;

    private String remark;

    private Date createdAt;

    private Date updatedAt;
}
