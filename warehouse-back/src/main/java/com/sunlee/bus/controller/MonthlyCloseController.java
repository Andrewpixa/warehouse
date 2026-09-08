package com.sunlee.bus.controller;

import com.sunlee.bus.service.IMonthlyCloseService;
import com.sunlee.sys.common.Constast;
import com.sunlee.sys.common.ResultObj;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/monthlyClose")
public class MonthlyCloseController {

    @Autowired
    private IMonthlyCloseService monthlyCloseService;

    @RequestMapping("loadStatement")
    public Map<String, Object> loadStatement(String yearMonth) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "ok");
            map.put("data", monthlyCloseService.loadStatement(yearMonth));
        } catch (Exception e) {
            log.error("查询月结对账失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @RequestMapping("confirm")
    public ResultObj confirm(String yearMonth) {
        try {
            monthlyCloseService.confirm(yearMonth);
            return ResultObj.ok("月结成功");
        } catch (Exception e) {
            log.error("月结失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }
}
