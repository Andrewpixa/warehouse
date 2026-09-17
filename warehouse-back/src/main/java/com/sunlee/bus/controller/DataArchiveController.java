package com.sunlee.bus.controller;

import com.sunlee.bus.service.IDataArchiveService;
import com.sunlee.sys.common.DataGridView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 业务数据归档 Controller
 * <p>
 * 提供 REST 接口手动触发归档和预览待归档数量:
 * - POST /archive/run?days=365  手动触发全量归档(days 可选,覆盖默认配置)
 * - GET  /archive/preview        预览每张表待归档数量(不执行迁移)
 */
@Slf4j
@RestController
@RequestMapping("/archive")
public class DataArchiveController {

    @Autowired
    private IDataArchiveService dataArchiveService;

    /**
     * 手动触发全量归档
     *
     * @param days 保留天数(可选,默认使用配置 archive.retention-days=365)
     * @return 每张表归档条数
     */
    @RequestMapping("/run")
    public DataGridView runArchive(@RequestParam(required = false) Integer days) {
        try {
            log.info("[数据归档] 收到手动触发请求, days={}", days);
            Map<String, Long> result = dataArchiveService.runArchive(days);
            List<Map<String, Object>> rows = new ArrayList<>();
            long total = 0;
            for (Map.Entry<String, Long> e : result.entrySet()) {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("tableName", e.getKey());
                row.put("archived", e.getValue());
                rows.add(row);
                if (e.getValue() > 0) total += e.getValue();
            }
            return new DataGridView(total, rows);
        } catch (Exception e) {
            log.error("[数据归档] 执行失败: {}", e.getMessage(), e);
            return new DataGridView(0L, "归档失败: " + e.getMessage());
        }
    }

    /**
     * 预览每张表待归档数量(不执行迁移)
     *
     * @param days 保留天数(可选,默认使用配置 archive.retention-days=365)
     * @return 8 张表各自待归档条数
     */
    @RequestMapping("/preview")
    public DataGridView previewArchive(@RequestParam(required = false) Integer days) {
        Map<String, Long> counts = dataArchiveService.previewArchive(days);
        // 转成 list of map 方便前端表格展示
        List<Map<String, Object>> rows = new ArrayList<>();
        long total = 0;
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("tableName", e.getKey());
            row.put("count", e.getValue());
            rows.add(row);
            total += e.getValue();
        }
        return new DataGridView(total, rows);
    }
}
