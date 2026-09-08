package com.sunlee.bus.vo;

import com.sunlee.bus.entity.Supplier;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class SupplierVo extends Supplier {

    private Integer page = 1;
    private Integer limit = 10;

    private Long[] ids;
}
