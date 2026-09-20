package com.sunlee.bus.controller;

import com.sunlee.bus.entity.SalesOrder;
import com.sunlee.bus.service.ISalesOrderService;
import com.sunlee.bus.service.ITraceCodeService;
import com.sunlee.bus.vo.TraceCodeVo;
import com.sunlee.bus.vo.TraceLookupResult;
import com.sunlee.sys.annotation.OperationLog;
import com.sunlee.sys.common.Constast;
import com.sunlee.sys.common.DataGridView;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/trace")
public class TraceCodeController {

    @Autowired
    private ITraceCodeService traceCodeService;

    @Autowired
    private ISalesOrderService salesOrderService;

    @RequestMapping("lookup")
    public Map<String, Object> lookup(String keyword) {
        Map<String, Object> map = new HashMap<>();
        try {
            TraceLookupResult result = traceCodeService.lookup(keyword);
            map.put("code", Constast.OK);
            map.put("msg", "ok");
            map.put("data", result);
        } catch (Exception e) {
            log.error("追溯码统一查询失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @RequestMapping("explainPack")
    public Map<String, Object> explainPack(String code) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "ok");
            map.put("data", traceCodeService.explainPack(code));
        } catch (Exception e) {
            log.error("解析大码失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @RequestMapping("loadInvoice")
    public Map<String, Object> loadInvoice(String invoiceNo) {
        Map<String, Object> map = new HashMap<>();
        try {
            SalesOrder order = salesOrderService.getByInvoiceNo(invoiceNo);
            map.put("code", Constast.OK);
            map.put("msg", "ok");
            if (order != null && order.getId() != null) {
                traceCodeService.ensureLogisticsCodes(order.getId());
            }
            map.put("data", order);
            map.put("traces", traceCodeService.listByInvoiceNo(invoiceNo));
        } catch (Exception e) {
            log.error("按发票号查询追溯码失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @RequestMapping("loadByInvoice")
    public DataGridView loadByInvoice(String invoiceNo) {
        return new DataGridView(traceCodeService.listByInvoiceNo(invoiceNo));
    }

    @RequestMapping("loadBySpdid")
    public DataGridView loadBySpdid(String spdid) {
        return new DataGridView(traceCodeService.listBySpdid(spdid));
    }

    @RequestMapping("previewParse")
    public DataGridView previewParse(String code) {
        try {
            List<?> rows = traceCodeService.previewParse(code);
            return new DataGridView((long) rows.size(), rows);
        } catch (Exception e) {
            log.error("预览包装解析失败: {}", e.getMessage(), e);
            DataGridView view = new DataGridView();
            view.setCode(Constast.ERROR);
            view.setMsg(e.getMessage());
            return view;
        }
    }

    @OperationLog(type = "修改", module = "追溯码", description = "'异常纠码 id: ' + #args[0].traceId")
    @RequestMapping("replaceAbnormal")
    public Map<String, Object> replaceAbnormal(@RequestBody TraceCodeVo vo) {
        Map<String, Object> map = new HashMap<>();
        try {
            int count = traceCodeService.replaceAbnormal(vo.getTraceId(), vo.getNewCode(),
                    Boolean.TRUE.equals(vo.getUseUniversal01()), vo.getRemark());
            map.put("code", Constast.OK);
            map.put("msg", Boolean.TRUE.equals(vo.getUseUniversal01()) ? "已用 01 码代替异常码" : "已用扫描码替换异常码");
            map.put("data", count);
        } catch (Exception e) {
            log.error("异常纠码失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "添加", module = "追溯码", description = "'缺码登记 SPDID: ' + #args[0].spdid")
    @RequestMapping("reportMissing")
    public Map<String, Object> reportMissing(@RequestBody TraceCodeVo vo) {
        Map<String, Object> map = new HashMap<>();
        try {
            int count = traceCodeService.reportMissing(vo.getSpdid(), vo.getCustomerNote());
            map.put("code", Constast.OK);
            map.put("msg", "已登记客户缺码说明");
            map.put("data", count);
        } catch (Exception e) {
            log.error("缺码登记失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "添加", module = "追溯码", description = "'采集追溯码 SPDID: ' + #args[0].spdid")
    @RequestMapping("addCodes")
    public Map<String, Object> addCodes(@RequestBody TraceCodeVo vo) {
        Map<String, Object> map = new HashMap<>();
        try {
            boolean skipDup = Boolean.TRUE.equals(vo.getSkipDuplicate());
            int count = traceCodeService.addCodes(vo.getSpdid(), vo.getCodes(), vo.getPackLevel(), vo.getBizType(), skipDup);
            String msg = "已采集 " + count + " 条追溯码";
            if (skipDup && vo.getCodes() != null) {
                long valid = vo.getCodes().stream().filter(StringUtils::isNotBlank).count();
                if (valid > count) {
                    msg += "，跳过重复 " + (valid - count) + " 条";
                }
            }
            map.put("code", Constast.OK);
            map.put("msg", msg);
            map.put("data", count);
        } catch (Exception e) {
            log.error("采集追溯码失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "添加", module = "追溯码", description = "'解析包装码: ' + #args[0].code")
    @RequestMapping("parseCodes")
    public Map<String, Object> parseCodes(@RequestBody TraceCodeVo vo) {
        Map<String, Object> map = new HashMap<>();
        try {
            int count = traceCodeService.parseAndCollect(vo.getSpdid(), vo.getCode(), vo.getBizType());
            map.put("code", Constast.OK);
            map.put("msg", "已解析写入 " + count + " 条追溯码");
        } catch (Exception e) {
            log.error("解析追溯码失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }
}
