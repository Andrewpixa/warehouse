package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.Member;
import com.sunlee.bus.entity.MemberLevelRule;
import com.sunlee.bus.entity.MemberRecord;
import com.sunlee.bus.mapper.MemberMapper;
import com.sunlee.bus.service.IMemberLevelRuleService;
import com.sunlee.bus.service.IMemberRecordService;
import com.sunlee.bus.service.IMemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@Transactional
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements IMemberService {

    @Autowired
    private IMemberRecordService memberRecordService;

    @Autowired
    private IMemberLevelRuleService memberLevelRuleService;

    @Override
    public void recharge(Integer memberId, BigDecimal amount, String operator, String remark) {
        Member member = getById(memberId);
        if (member == null || member.getStatus() != 1) {
            throw new RuntimeException("会员不存在或已禁用");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("充值金额必须大于0");
        }

        // 原子更新余额与累计充值，防止并发充值丢失更新
        if (baseMapper.addRecharge(memberId, amount) == 0) {
            throw new RuntimeException("充值失败，请重试");
        }

        // 重新读取更新后的数据（用于等级计算与流水余额），等级只升不降，且只回写等级字段，避免覆盖并发更新
        Member updated = getById(memberId);
        upgradeLevelIfNeeded(updated);

        saveRecord(memberId, "充值", amount, updated.getBalance(), operator, remark);
    }

    @Override
    public void consume(Integer memberId, BigDecimal amount, String operator, String remark) {
        Member member = getById(memberId);
        if (member == null || member.getStatus() != 1) {
            throw new RuntimeException("会员不存在或已禁用");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("消费金额必须大于0");
        }

        // 原子扣减余额（余额不足时不更新），同时累加消费额与积分（1元=1积分）
        if (baseMapper.deductBalance(memberId, amount, amount.intValue()) == 0) {
            throw new RuntimeException("余额不足，当前余额: " + member.getBalance());
        }

        Member updated = getById(memberId);
        // 等级规则含累计消费条件，消费后同样需要重算等级（只升不降）
        upgradeLevelIfNeeded(updated);
        saveRecord(memberId, "消费", amount, updated.getBalance(), operator, remark);
    }

    @Override
    public void refund(Integer memberId, BigDecimal amount, String operator, String remark) {
        Member member = getById(memberId);
        if (member == null) {
            throw new RuntimeException("会员不存在");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("退款金额必须大于0");
        }

        // 原子增加余额，防止并发退款丢失更新
        if (baseMapper.addBalance(memberId, amount) == 0) {
            throw new RuntimeException("退款失败，请重试");
        }

        Member updated = getById(memberId);
        saveRecord(memberId, "退款", amount, updated.getBalance(), operator, remark);
    }

    private void saveRecord(Integer memberId, String type, BigDecimal amount, BigDecimal balanceAfter, String operator, String remark) {
        MemberRecord record = new MemberRecord();
        record.setMemberId(memberId);
        record.setType(type);
        record.setAmount(amount);
        record.setBalanceAfter(balanceAfter);
        record.setOperator(operator);
        record.setCreateTime(new Date());
        record.setRemark(remark);
        memberRecordService.save(record);
    }

    /**
     * 按等级规则重算会员等级，达到更高等级时只回写 level 字段（只升不降，避免覆盖并发更新）
     */
    private void upgradeLevelIfNeeded(Member member) {
        Integer newLevel = computeLevel(member.getTotalRecharge(), member.getTotalConsume());
        Integer currentLevel = member.getLevel() != null ? member.getLevel() : 0;
        if (newLevel > currentLevel) {
            Member levelUpdate = new Member();
            levelUpdate.setId(member.getId());
            levelUpdate.setLevel(newLevel);
            updateById(levelUpdate);
        }
    }

    /**
     * 根据 bus_member_level_rule 配置的规则计算会员等级：
     * 按 level_value 从高到低取第一个达标的规则；规则未配置时回退到默认阈值（500/2000/5000）。
     * 条件关系 conditionType: 1满足其一（充值或消费任一达标） 2同时满足。
     * 某档阈值未配置(null)时：满足其一模式下该腿不参与，同时满足模式下该腿视为达标。
     */
    private Integer computeLevel(BigDecimal totalRecharge, BigDecimal totalConsume) {
        List<MemberLevelRule> rules = memberLevelRuleService.list(
                new QueryWrapper<MemberLevelRule>().orderByDesc("level_value"));
        BigDecimal recharge = totalRecharge != null ? totalRecharge : BigDecimal.ZERO;
        BigDecimal consume = totalConsume != null ? totalConsume : BigDecimal.ZERO;
        if (rules.isEmpty()) {
            if (recharge.compareTo(new BigDecimal("5000")) >= 0) {
                return 4;
            } else if (recharge.compareTo(new BigDecimal("2000")) >= 0) {
                return 3;
            } else if (recharge.compareTo(new BigDecimal("500")) >= 0) {
                return 2;
            }
            return 1;
        }
        for (MemberLevelRule rule : rules) {
            if (rule.getLevelValue() == null) {
                continue;
            }
            boolean hasRecharge = rule.getMinRecharge() != null;
            boolean hasConsume = rule.getMinConsume() != null;
            boolean rechargeOk = hasRecharge && recharge.compareTo(rule.getMinRecharge()) >= 0;
            boolean consumeOk = hasConsume && consume.compareTo(rule.getMinConsume()) >= 0;
            boolean match;
            if (Integer.valueOf(2).equals(rule.getConditionType())) {
                // 同时满足：已配置的条件全部达标（都未配置视为达标）
                match = (!hasRecharge || rechargeOk) && (!hasConsume || consumeOk);
            } else {
                // 满足其一：任一已配置条件达标（都未配置则不达标）
                match = rechargeOk || consumeOk;
            }
            if (match) {
                return rule.getLevelValue();
            }
        }
        return 1;
    }

    /**
     * 生成会员卡号: VIP + 日期 + 4位序号
     */
    public static String generateMemberNo(int seq) {
        return "VIP" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + String.format("%04d", seq);
    }
}
