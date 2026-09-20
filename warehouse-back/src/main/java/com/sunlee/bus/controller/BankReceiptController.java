package com.sunlee.bus.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sunlee.bus.entity.BankReceipt;
import com.sunlee.bus.service.IBankReceiptService;
import com.sunlee.bus.vo.BankReceiptVo;
import com.sunlee.sys.annotation.OperationLog;
import com.sunlee.sys.common.Constast;
import com.sunlee.sys.common.DataGridView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/bankReceipt")
public class BankReceiptController {

    @Autowired
    private IBankReceiptService bankReceiptService;

    @RequestMapping("loadAll")
    public DataGridView loadAll(BankReceiptVo vo) {
        IPage<BankReceipt> page = bankReceiptService.pageReceipts(vo);
        return new DataGridView(page.getTotal(), page.getRecords());
    }

    @OperationLog(type = "添加", module = "银行到账", description = "'登记到账: ' + #args[0].receiptNo")
    @RequestMapping("save")
    public Map<String, Object> save(@RequestBody BankReceiptVo vo) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "已登记到账");
            map.put("data", bankReceiptService.saveReceipt(vo));
        } catch (Exception e) {
            log.error("登记到账失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }
}
