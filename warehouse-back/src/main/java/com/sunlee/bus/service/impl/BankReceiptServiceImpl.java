package com.sunlee.bus.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.common.PharmaNos;
import com.sunlee.bus.entity.BankReceipt;
import com.sunlee.bus.entity.Customer;
import com.sunlee.bus.mapper.BankReceiptMapper;
import com.sunlee.bus.service.IBankReceiptService;
import com.sunlee.bus.service.ICustomerService;
import com.sunlee.bus.vo.BankReceiptVo;
import com.sunlee.sys.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Service
public class BankReceiptServiceImpl extends ServiceImpl<BankReceiptMapper, BankReceipt> implements IBankReceiptService {

    @Autowired
    private ICustomerService customerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public IPage<BankReceipt> pageReceipts(BankReceiptVo vo) {
        IPage<BankReceipt> page = new Page<>(vo.getPage(), vo.getLimit());
        QueryWrapper<BankReceipt> qw = new QueryWrapper<>();
        qw.eq(vo.getCustomerId() != null, "customer_id", vo.getCustomerId());
        qw.eq(StringUtils.isNotBlank(vo.getStatus()), "status", vo.getStatus());
        qw.eq(StringUtils.isNotBlank(vo.getChannel()), "channel", vo.getChannel());
        qw.like(StringUtils.isNotBlank(vo.getReceiptNo()), "receipt_no", vo.getReceiptNo());
        qw.like(StringUtils.isNotBlank(vo.getVoucherNo()), "voucher_no", vo.getVoucherNo());
        qw.orderByDesc("id");
        this.page(page, qw);
        page.getRecords().forEach(this::fill);
        return page;
    }

    @Override
    @Transactional
    public BankReceipt saveReceipt(BankReceiptVo vo) {
        if (vo.getCustomerId() == null) {
            throw new IllegalArgumentException("请选择付款客户");
        }
        if (vo.getAmount() == null || vo.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("到账金额必须大于 0");
        }
        if (vo.getReceivedDate() == null) {
            vo.setReceivedDate(LocalDate.now());
        }
        if (StringUtils.isBlank(vo.getChannel())) {
            vo.setChannel("公对公");
        }
        if (!"公对公".equals(vo.getChannel()) && !"支票".equals(vo.getChannel())) {
            throw new IllegalArgumentException("渠道只能是公对公或支票");
        }
        if (StringUtils.isBlank(vo.getReceiptNo())) {
            vo.setReceiptNo(PharmaNos.orderNo("BR"));
        }
        User user = currentUser();
        Date now = new Date();
        if (vo.getId() == null) {
            vo.setCreatedBy(user == null ? null : user.getId().longValue());
            vo.setCreatedAt(now);
            if (StringUtils.isBlank(vo.getStatus())) {
                vo.setStatus("未认领");
            }
        } else {
            BankReceipt old = this.getById(vo.getId());
            if (old == null) {
                throw new IllegalArgumentException("到账流水不存在");
            }
            BigDecimal used = allocatedAmount(vo.getId(), null);
            if (vo.getAmount().compareTo(used) < 0) {
                throw new IllegalArgumentException("到账金额不能小于已认领金额 " + used);
            }
        }
        vo.setUpdatedAt(now);
        this.saveOrUpdate(vo);
        refreshStatus(vo.getId());
        BankReceipt saved = this.getById(vo.getId());
        fill(saved);
        return saved;
    }

    @Override
    public BigDecimal allocatedAmount(Long receiptId, Long excludeDocId) {
        if (receiptId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal n;
        if (excludeDocId == null) {
            n = jdbcTemplate.queryForObject("""
                    SELECT IFNULL(SUM(i.amount), 0)
                    FROM ops_flow_items i
                    INNER JOIN ops_flow_docs d ON d.id = i.doc_id
                    WHERE d.doc_type = 'OFFSET' AND d.status = '已冲账'
                      AND IFNULL(d.hang_flag, 0) = 0 AND d.receipt_id = ?
                    """, BigDecimal.class, receiptId);
        } else {
            n = jdbcTemplate.queryForObject("""
                    SELECT IFNULL(SUM(i.amount), 0)
                    FROM ops_flow_items i
                    INNER JOIN ops_flow_docs d ON d.id = i.doc_id
                    WHERE d.doc_type = 'OFFSET' AND d.status = '已冲账'
                      AND IFNULL(d.hang_flag, 0) = 0 AND d.receipt_id = ? AND d.id <> ?
                    """, BigDecimal.class, receiptId, excludeDocId);
        }
        return n == null ? BigDecimal.ZERO : n;
    }

    @Override
    public void refreshStatus(Long receiptId) {
        if (receiptId == null) {
            return;
        }
        BankReceipt r = this.getById(receiptId);
        if (r == null) {
            return;
        }
        BigDecimal allocated = allocatedAmount(receiptId, null);
        Integer hang = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ops_flow_docs WHERE doc_type='OFFSET' AND status='已挂账' AND receipt_id=?",
                Integer.class, receiptId);
        String status;
        if (hang != null && hang > 0 && allocated.compareTo(r.getAmount()) < 0) {
            status = "挂账";
        } else if (allocated.compareTo(BigDecimal.ZERO) <= 0) {
            status = "未认领";
        } else if (allocated.compareTo(r.getAmount()) >= 0) {
            status = "已认领";
        } else {
            status = "部分认领";
        }
        r.setStatus(status);
        r.setUpdatedAt(new Date());
        this.updateById(r);
    }

    @Override
    public long countOpen() {
        return this.count(new QueryWrapper<BankReceipt>().in("status", "未认领", "部分认领", "挂账"));
    }

    private void fill(BankReceipt r) {
        if (r.getCustomerId() != null) {
            Customer c = customerService.getById(r.getCustomerId());
            if (c != null) {
                r.setCustomerName(c.getName());
            }
        }
        BigDecimal allocated = allocatedAmount(r.getId(), null);
        r.setAllocatedAmount(allocated);
        BigDecimal amt = r.getAmount() == null ? BigDecimal.ZERO : r.getAmount();
        r.setRemainAmount(amt.subtract(allocated));
    }

    private User currentUser() {
        try {
            return (User) StpUtil.getSession().get("user");
        } catch (Exception e) {
            return null;
        }
    }
}
