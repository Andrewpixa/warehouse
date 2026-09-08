package com.sunlee.bus.controller;

import com.sunlee.bus.service.IDailyCloseService;
import com.sunlee.sys.common.Constast;
import com.sunlee.sys.common.ResultObj;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/dailyClose")
public class DailyCloseController {

    @Autowired
    private IDailyCloseService dailyCloseService;

    @RequestMapping("loadChecklist")
    public Map<String, Object> loadChecklist(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bizDate) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "ok");
            map.put("data", dailyCloseService.loadChecklist(bizDate));
        } catch (Exception e) {
            log.error("查询日清清单失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @RequestMapping("confirm")
    public ResultObj confirm(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bizDate) {
        try {
            dailyCloseService.confirm(bizDate);
            return ResultObj.ok("日清成功");
        } catch (Exception e) {
            log.error("日清失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }
}
