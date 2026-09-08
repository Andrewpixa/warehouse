package com.sunlee.bus.vo;

import com.sunlee.bus.entity.Drug;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class DrugVo extends Drug {

    private Integer page = 1;
    private Integer limit = 10;

    private Long[] ids;
}
