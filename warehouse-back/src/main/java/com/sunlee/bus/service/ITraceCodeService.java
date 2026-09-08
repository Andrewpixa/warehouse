package com.sunlee.bus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.TraceCode;

import java.util.List;

public interface ITraceCodeService extends IService<TraceCode> {

    List<TraceCode> listByInvoiceNo(String invoiceNo);

    List<TraceCode> listBySpdid(String spdid);

    int addCodes(String spdid, List<String> codes, String packLevel, String bizType);

    /** 按预置关联展开大/中包装，不落库 */
    List<TraceCode> previewParse(String parentCode);

    /** 解析并写入当前 SPDID */
    int parseAndCollect(String spdid, String parentCode, String bizType);
}
