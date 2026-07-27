package com.sunlee.bus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.WarehouseWarnRule;

/**
 * 分仓预警规则服务
 *
 * @author sunlee
 * @since 2026-07-26
 */
public interface IWarehouseWarnRuleService extends IService<WarehouseWarnRule> {

    /**
     * 保存规则（同商品同仓库已存在则更新阈值）
     */
    void saveRule(WarehouseWarnRule rule);
}
