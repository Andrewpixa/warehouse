package com.sunlee.bus.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.OpsFlowDoc;
import com.sunlee.bus.vo.OpsFlowDocVo;

import java.util.List;
import java.util.Map;

public interface IOpsFlowService extends IService<OpsFlowDoc> {

    IPage<OpsFlowDoc> pageDocs(OpsFlowDocVo vo);

    OpsFlowDoc getDetail(Long id);

    OpsFlowDoc saveDraft(OpsFlowDocVo vo);

    void confirm(Long id, String action);

    void deleteDraft(Long id);

    Map<String, Object> workbench();

    Map<String, Object> printPack(String invoiceNo);

    Map<String, Object> makerQuery(String loginName, String password, String from, String to);

    List<Map<String, Object>> makerAccounts();
}
