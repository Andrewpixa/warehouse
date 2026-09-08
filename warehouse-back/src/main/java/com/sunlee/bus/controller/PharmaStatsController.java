package com.sunlee.bus.controller;

import com.sunlee.bus.service.IPharmaStatsService;
import com.sunlee.sys.common.Constast;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/pharmaStats")
public class PharmaStatsController {

    @Autowired
    private IPharmaStatsService pharmaStatsService;

    @RequestMapping("purchaseStats")
    public Map<String, Object> purchaseStats(String startDate, String endDate) {
        return wrap(() -> pharmaStatsService.purchaseStats(startDate, endDate), "查询进货统计失败");
    }

    @RequestMapping("salesStats")
    public Map<String, Object> salesStats(String startDate, String endDate) {
        return wrap(() -> pharmaStatsService.salesStats(startDate, endDate), "查询出货统计失败");
    }

    private Map<String, Object> wrap(java.util.function.Supplier<Object> supplier, String errorMsg) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "ok");
            map.put("data", supplier.get());
        } catch (Exception e) {
            log.error("{}: {}", errorMsg, e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }
}
