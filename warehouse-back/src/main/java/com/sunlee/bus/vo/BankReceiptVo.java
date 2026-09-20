package com.sunlee.bus.vo;

import com.sunlee.bus.entity.BankReceipt;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class BankReceiptVo extends BankReceipt {

    private Integer page = 1;

    private Integer limit = 10;
}
