package com.sunlee.bus.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sunlee.bus.entity.OperationLog;
import com.sunlee.bus.service.IOperationLogService;
import com.sunlee.bus.vo.OperationLogVo;
import com.sunlee.sys.common.DataGridView;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@Slf4j
@RestController
@RequestMapping("/operationLog")
public class OperationLogController {

    @Autowired
    private IOperationLogService operationLogService;

    @RequestMapping("loadAllOperationLog")
    public DataGridView loadAllOperationLog(OperationLogVo operationLogVo) {
        try {
            IPage<OperationLog> page = new Page<>(operationLogVo.getPage(), operationLogVo.getLimit());
            QueryWrapper<OperationLog> queryWrapper = new QueryWrapper<>();
            queryWrapper.like(StringUtils.isNotBlank(operationLogVo.getType()), "type", operationLogVo.getType());
            queryWrapper.like(StringUtils.isNotBlank(operationLogVo.getModule()), "module", operationLogVo.getModule());
            queryWrapper.like(StringUtils.isNotBlank(operationLogVo.getOperateperson()), "operateperson", operationLogVo.getOperateperson());
            queryWrapper.ge(operationLogVo.getStartTime() != null, "operatetime", operationLogVo.getStartTime());
            queryWrapper.le(operationLogVo.getEndTime() != null, "operatetime", operationLogVo.getEndTime());
            queryWrapper.orderByDesc("operatetime");
            operationLogService.page(page, queryWrapper);
            return new DataGridView(page.getTotal(), page.getRecords());
        } catch (Exception e) {
            log.warn("操作日志暂不可用: {}", e.getMessage());
            return new DataGridView(0L, Collections.emptyList());
        }
    }
}
