package com.sunlee.bus.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sunlee.bus.entity.BatchStock;
import com.sunlee.bus.service.IBatchStockService;
import com.sunlee.bus.vo.BatchStockVo;
import com.sunlee.sys.common.DataGridView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/batchStock")
public class BatchStockController {

    @Autowired
    private IBatchStockService batchStockService;

    @RequestMapping("loadAllBatchStock")
    public DataGridView loadAllBatchStock(BatchStockVo vo) {
        IPage<BatchStock> page = batchStockService.pageStock(vo);
        return new DataGridView(page.getTotal(), page.getRecords());
    }
}
