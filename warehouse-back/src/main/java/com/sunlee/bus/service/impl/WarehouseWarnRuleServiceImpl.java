package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.WarehouseWarnRule;
import com.sunlee.bus.mapper.WarehouseWarnRuleMapper;
import com.sunlee.bus.service.IWarehouseWarnRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 分仓预警规则服务实现
 *
 * @author sunlee
 * @since 2026-07-26
 */
@Service
@Transactional
public class WarehouseWarnRuleServiceImpl extends ServiceImpl<WarehouseWarnRuleMapper, WarehouseWarnRule>
        implements IWarehouseWarnRuleService {

    @Override
    public void saveRule(WarehouseWarnRule rule) {
        if (rule.getGoodsid() == null || rule.getWarehouseId() == null) {
            throw new RuntimeException("商品与仓库不能为空");
        }
        if (rule.getDangernum() == null || rule.getDangernum() < 0) {
            throw new RuntimeException("预警阈值不能为负数");
        }
        // 同商品同仓库的规则唯一，已存在则更新阈值
        QueryWrapper<WarehouseWarnRule> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("goodsid", rule.getGoodsid());
        queryWrapper.eq("warehouse_id", rule.getWarehouseId());
        WarehouseWarnRule exist = baseMapper.selectOne(queryWrapper);
        if (exist != null) {
            exist.setDangernum(rule.getDangernum());
            updateById(exist);
        } else {
            save(rule);
        }
    }
}
