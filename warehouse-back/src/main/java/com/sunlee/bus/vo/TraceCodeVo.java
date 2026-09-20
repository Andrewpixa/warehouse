package com.sunlee.bus.vo;

import com.sunlee.bus.entity.TraceCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class TraceCodeVo extends TraceCode {

    private Integer page = 1;
    private Integer limit = 10;

    private List<String> codes;

    /** 扫码采集场景：true 时重复码跳过不报错（默认 false 保持严格防重） */
    private Boolean skipDuplicate = false;

    /** 要替换的原码 id */
    private Long traceId;

    /** 纠错后的新码；useUniversal01 时由服务端生成 01 码 */
    private String newCode;

    private Boolean useUniversal01 = false;

    /** 缺码时客户问题描述 */
    private String customerNote;
}
