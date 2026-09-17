package com.sunlee.bus.schedule;

import com.sunlee.bus.service.IDataArchiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 业务数据归档定时任务
 * <p>
 * 每天 03:00 自动执行,使用配置 archive.retention-days 作为保留天数(默认 365)
 * 异常全部 catch 不抛出,避免调度器禁用任务
 */
@Slf4j
@Component
public class ArchiveSchedule {

    @Autowired
    private IDataArchiveService dataArchiveService;

    /**
     * 每天凌晨 3 点执行全量归档
     * cron: 秒 分 时 日 月 周 (Quartz 6 段格式)
     * zone: Asia/Shanghai
     */
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Shanghai")
    public void runDailyArchive() {
        log.info("[数据归档-定时] 每天 03:00 自动触发,开始执行");
        try {
            Map<String, Long> result = dataArchiveService.runArchive(null);
            log.info("[数据归档-定时] 执行完成,归档结果={}", result);
        } catch (Exception e) {
            log.error("[数据归档-定时] 执行异常: {}", e.getMessage(), e);
        }
    }
}
