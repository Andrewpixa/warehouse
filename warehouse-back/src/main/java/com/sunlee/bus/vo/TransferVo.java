package com.sunlee.bus.vo;

import com.sunlee.bus.entity.Transfer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class TransferVo extends Transfer {

    private Integer page = 1;
    private Integer limit = 10;
}
