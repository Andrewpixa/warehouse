package com.sunlee.bus.controller;

import com.sunlee.bus.entity.BizAttachment;
import com.sunlee.bus.entity.BizSignature;
import com.sunlee.bus.service.IBizVoucherService;
import com.sunlee.sys.annotation.OperationLog;
import com.sunlee.sys.common.Constast;
import com.sunlee.sys.common.ResultObj;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/voucher")
public class BizVoucherController {

    @Autowired
    private IBizVoucherService voucherService;

    @RequestMapping("loadVoucher")
    public Map<String, Object> loadVoucher(String bizType, Long bizId) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("code", Constast.OK);
            map.put("msg", "ok");
            Map<String, Object> data = new HashMap<>();
            data.put("attachments", voucherService.listAttachments(bizType, bizId));
            data.put("signatures", voucherService.listSignatures(bizType, bizId));
            map.put("data", data);
        } catch (Exception e) {
            log.error("查询票据签字失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "添加", module = "单据票据", description = "'上传附件 ' + #args[0] + '/' + #args[2]")
    @RequestMapping("saveAttachment")
    public Map<String, Object> saveAttachment(String bizType, Long bizId, String attachType, String filePath, String fileName) {
        Map<String, Object> map = new HashMap<>();
        try {
            BizAttachment row = voucherService.saveAttachment(bizType, bizId, attachType, filePath, fileName);
            map.put("code", Constast.OK);
            map.put("msg", "已保存");
            map.put("data", row);
        } catch (Exception e) {
            log.error("保存附件失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "删除", module = "单据票据", description = "'删除附件ID: ' + #args[0]")
    @RequestMapping("deleteAttachment")
    public ResultObj deleteAttachment(Long id) {
        try {
            voucherService.deleteAttachment(id);
            return ResultObj.DELETE_SUCCESS;
        } catch (Exception e) {
            log.error("删除附件失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }

    @OperationLog(type = "添加", module = "单据签字", description = "'保存签字 ' + #args[0] + '/' + #args[2]")
    @RequestMapping("saveSignature")
    public Map<String, Object> saveSignature(String bizType, Long bizId, String signRole,
                                             String signerName, String signSource, String imagePath) {
        Map<String, Object> map = new HashMap<>();
        try {
            BizSignature row = voucherService.saveSignature(bizType, bizId, signRole, signerName, signSource, imagePath);
            map.put("code", Constast.OK);
            map.put("msg", "签字已保存");
            map.put("data", row);
        } catch (Exception e) {
            log.error("保存签字失败: {}", e.getMessage(), e);
            map.put("code", Constast.ERROR);
            map.put("msg", e.getMessage());
        }
        return map;
    }

    @OperationLog(type = "删除", module = "单据签字", description = "'删除签字ID: ' + #args[0]")
    @RequestMapping("deleteSignature")
    public ResultObj deleteSignature(Long id) {
        try {
            voucherService.deleteSignature(id);
            return ResultObj.DELETE_SUCCESS;
        } catch (Exception e) {
            log.error("删除签字失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }
}
