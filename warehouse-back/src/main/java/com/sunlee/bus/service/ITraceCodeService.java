package com.sunlee.bus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.TraceCode;
import com.sunlee.bus.vo.TraceLookupResult;

import java.util.List;

public interface ITraceCodeService extends IService<TraceCode> {

    /**
     * 统一查询：追溯码、SPDID、采购单号、供应商发票、批号、出库单号、销售发票。
     */
    TraceLookupResult lookup(String keyword);

    List<TraceCode> listByInvoiceNo(String invoiceNo);

    List<TraceCode> listBySpdid(String spdid);

    /** 已确认出库单按物流匹配挂码（无码才补），业务员不采集 */
    void ensureLogisticsCodes(Long salesOrderId);

    /** 错码/异常：扫正确码或用 01 码代替 */
    int replaceAbnormal(Long traceId, String newCode, boolean useUniversal01, String remark);

    /** 缺码：只登记客户描述，不扫码补数 */
    int reportMissing(String spdid, String customerNote);

    int addCodes(String spdid, List<String> codes, String packLevel, String bizType);

    /**
     * 批量采集追溯码
     *
     * @param skipDuplicate true：重复码跳过并继续（扫码场景）；false：遇重复码整批报错
     * @return 实际新增条数
     */
    int addCodes(String spdid, List<String> codes, String packLevel, String bizType, boolean skipDuplicate);

    /** 按预置关联展开大/中包装，不落库 */
    List<TraceCode> previewParse(String parentCode);

    /** 解析并写入当前 SPDID */
    int parseAndCollect(String spdid, String parentCode, String bizType);

    /** 输入大码（或中/小码）看包装树、批号、入出库履历 */
    com.sunlee.bus.vo.TracePackExplainVo explainPack(String code);
}
