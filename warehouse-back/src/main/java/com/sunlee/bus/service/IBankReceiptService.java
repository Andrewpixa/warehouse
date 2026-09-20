package com.sunlee.bus.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.BankReceipt;
import com.sunlee.bus.vo.BankReceiptVo;

import java.math.BigDecimal;

public interface IBankReceiptService extends IService<BankReceipt> {

    IPage<BankReceipt> pageReceipts(BankReceiptVo vo);

    BankReceipt saveReceipt(BankReceiptVo vo);

    BigDecimal allocatedAmount(Long receiptId, Long excludeDocId);

    void refreshStatus(Long receiptId);

    long countOpen();
}
