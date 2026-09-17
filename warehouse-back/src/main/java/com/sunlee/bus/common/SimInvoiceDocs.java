package com.sunlee.bus.common;

import cn.hutool.core.io.FileUtil;
import com.sunlee.sys.common.AppFileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 课设/演示用模拟票据：供应商随货发票、我方销售电子发票。不是税控盘真实开票。
 */
public final class SimInvoiceDocs {

    private SimInvoiceDocs() {
    }

    public static String supplierInvoiceNo(Long supplierId, LocalDate bizDate, int seq) {
        long sid = supplierId == null ? 0 : supplierId;
        LocalDate d = bizDate == null ? LocalDate.now() : bizDate;
        int n = seq < 1 ? 1 : seq;
        return String.format("FP%06d%s%04d", sid % 1_000_000, d.toString().replace("-", ""), n);
    }

    public static String writeHtml(String suggestedName, String title, String htmlBody) {
        String fileName = AppFileUtils.createNewFileName(suggestedName);
        File dest = new File(AppFileUtils.UPLOAD_PATH, fileName);
        String html = "<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\"/><title>"
                + escape(title) + "</title><style>body{font-family:sans-serif;padding:24px;color:#111}"
                + "h1{font-size:20px}table{border-collapse:collapse;width:100%}td,th{border:1px solid #ddd;padding:6px}"
                + ".hint{color:#666;font-size:12px;margin-top:16px}</style></head><body>"
                + htmlBody + "<p class=\"hint\">本文件为系统模拟票据，仅用于进销存演示，不能作为报税凭证。</p></body></html>";
        FileUtil.writeString(html, dest, StandardCharsets.UTF_8);
        return fileName;
    }

    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
