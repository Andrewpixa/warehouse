package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.Drug;
import com.sunlee.bus.entity.SalesOrder;
import com.sunlee.bus.entity.SalesOrderItem;
import com.sunlee.bus.entity.TraceCode;
import com.sunlee.bus.entity.TracePackRelation;
import com.sunlee.bus.mapper.TraceCodeMapper;
import com.sunlee.bus.service.IDrugService;
import com.sunlee.bus.service.ISalesOrderItemService;
import com.sunlee.bus.service.ISalesOrderService;
import com.sunlee.bus.service.ITraceCodeService;
import com.sunlee.bus.service.ITracePackRelationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class TraceCodeServiceImpl extends ServiceImpl<TraceCodeMapper, TraceCode> implements ITraceCodeService {

    @Autowired
    @Lazy
    private ISalesOrderService salesOrderService;

    @Autowired
    private ISalesOrderItemService salesOrderItemService;

    @Autowired
    private IDrugService drugService;

    @Autowired
    private ITracePackRelationService tracePackRelationService;

    @Override
    public List<TraceCode> listByInvoiceNo(String invoiceNo) {
        if (StringUtils.isBlank(invoiceNo)) {
            return new ArrayList<>();
        }
        QueryWrapper<SalesOrder> orderQw = new QueryWrapper<>();
        orderQw.eq("invoice_no", invoiceNo.trim());
        orderQw.last("LIMIT 1");
        SalesOrder order = salesOrderService.getOne(orderQw, false);
        if (order == null) {
            return new ArrayList<>();
        }
        QueryWrapper<SalesOrderItem> itemQw = new QueryWrapper<>();
        itemQw.eq("order_id", order.getId());
        List<SalesOrderItem> items = salesOrderItemService.list(itemQw);
        List<TraceCode> result = new ArrayList<>();
        for (SalesOrderItem item : items) {
            if (StringUtils.isBlank(item.getSpdid())) {
                continue;
            }
            List<TraceCode> codes = listBySpdid(item.getSpdid());
            Drug drug = drugService.getById(item.getDrugId());
            for (TraceCode code : codes) {
                code.setInvoiceNo(order.getInvoiceNo());
                code.setOrderNo(order.getOrderNo());
                code.setBatchNo(item.getBatchNo());
                if (drug != null) {
                    code.setDrugName(drug.getGenericName());
                }
                result.add(code);
            }
        }
        return result;
    }

    @Override
    public List<TraceCode> listBySpdid(String spdid) {
        if (StringUtils.isBlank(spdid)) {
            return new ArrayList<>();
        }
        QueryWrapper<TraceCode> qw = new QueryWrapper<>();
        qw.eq("spdid", spdid);
        qw.orderByAsc("id");
        return this.list(qw);
    }

    @Override
    @Transactional
    public int addCodes(String spdid, List<String> codes, String packLevel, String bizType) {
        if (StringUtils.isBlank(spdid)) {
            throw new IllegalArgumentException("SPDID 不能为空");
        }
        if (codes == null || codes.isEmpty()) {
            throw new IllegalArgumentException("请至少录入一条追溯码");
        }
        Date now = new Date();
        int saved = 0;
        for (String raw : codes) {
            if (StringUtils.isBlank(raw)) {
                continue;
            }
            String code = raw.trim();
            QueryWrapper<TraceCode> existQw = new QueryWrapper<>();
            existQw.eq("code", code);
            if (this.count(existQw) > 0) {
                throw new IllegalArgumentException("追溯码已存在：" + code);
            }
            TraceCode row = new TraceCode();
            row.setSpdid(spdid.trim());
            row.setCode(code);
            row.setPackLevel(StringUtils.defaultIfBlank(packLevel, "最小包装"));
            row.setBizType(StringUtils.defaultIfBlank(bizType, "出库"));
            row.setStatus("正常");
            row.setCollectedAt(now);
            this.save(row);
            saved++;
        }
        if (saved == 0) {
            throw new IllegalArgumentException("没有可保存的追溯码");
        }
        return saved;
    }

    @Override
    public List<TraceCode> previewParse(String parentCode) {
        return expandPack(parentCode);
    }

    @Override
    @Transactional
    public int parseAndCollect(String spdid, String parentCode, String bizType) {
        if (StringUtils.isBlank(spdid)) {
            throw new IllegalArgumentException("SPDID 不能为空");
        }
        List<TraceCode> expanded = expandPack(parentCode);
        Date now = new Date();
        int saved = 0;
        for (TraceCode row : expanded) {
            QueryWrapper<TraceCode> existQw = new QueryWrapper<>();
            existQw.eq("code", row.getCode());
            TraceCode exist = this.getOne(existQw, false);
            if (exist != null) {
                if (!spdid.trim().equals(exist.getSpdid())) {
                    throw new IllegalArgumentException("追溯码已被其他明细占用：" + row.getCode());
                }
                if (StringUtils.isBlank(exist.getParentCode()) && StringUtils.isNotBlank(row.getParentCode())) {
                    exist.setParentCode(row.getParentCode());
                    exist.setPackLevel(row.getPackLevel());
                    this.updateById(exist);
                }
                continue;
            }
            row.setSpdid(spdid.trim());
            row.setBizType(StringUtils.defaultIfBlank(bizType, "出库"));
            row.setStatus("正常");
            row.setCollectedAt(now);
            this.save(row);
            saved++;
        }
        if (saved == 0) {
            throw new IllegalArgumentException("解析结果均已采集，无需重复写入");
        }
        return saved;
    }

    private List<TraceCode> expandPack(String parentCode) {
        if (StringUtils.isBlank(parentCode)) {
            throw new IllegalArgumentException("请输入要解析的包装码");
        }
        String root = parentCode.trim();
        List<TracePackRelation> children = listChildren(root);
        if (children.isEmpty()) {
            throw new IllegalArgumentException("未找到该码的下级包装关系，最小包装不可再解析");
        }
        List<TraceCode> result = new ArrayList<>();
        TraceCode rootRow = new TraceCode();
        rootRow.setCode(root);
        rootRow.setPackLevel(children.get(0).getParentLevel());
        result.add(rootRow);
        walkChildren(root, result);
        return result;
    }

    private void walkChildren(String parent, List<TraceCode> out) {
        for (TracePackRelation rel : listChildren(parent)) {
            TraceCode child = new TraceCode();
            child.setCode(rel.getChildCode());
            child.setPackLevel(rel.getChildLevel());
            child.setParentCode(parent);
            out.add(child);
            walkChildren(rel.getChildCode(), out);
        }
    }

    private List<TracePackRelation> listChildren(String parentCode) {
        QueryWrapper<TracePackRelation> qw = new QueryWrapper<>();
        qw.eq("parent_code", parentCode);
        qw.orderByAsc("id");
        return tracePackRelationService.list(qw);
    }
}
