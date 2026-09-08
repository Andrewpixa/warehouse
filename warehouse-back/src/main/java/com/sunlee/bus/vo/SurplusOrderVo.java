package com.sunlee.bus.vo;

import com.sunlee.bus.entity.SurplusOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class SurplusOrderVo extends SurplusOrder {

    private Integer page = 1;
    private Integer limit = 10;
}
