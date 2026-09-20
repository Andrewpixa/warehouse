package com.sunlee.bus.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 演示环境每天 08:00 生成固定多明细入库/出库；启动时补当天（已存在则跳过）。
 */
@Slf4j
@Order(50)
@Component
@ConditionalOnProperty(prefix = "demo.daily-io", name = "enabled", havingValue = "true")
public class DemoDailyIoSchedule implements CommandLineRunner {

    @Autowired
    private DemoDailyIoService demoDailyIoService;

    @Override
    public void run(String... args) {
        runDaily();
    }

    @Scheduled(cron = "${demo.daily-io.cron:0 0 8 * * ?}", zone = "Asia/Shanghai")
    public void runDaily() {
        try {
            demoDailyIoService.runToday();
        } catch (Exception e) {
            log.warn("demo daily-io skipped: {}", e.getMessage());
        }
    }
}
