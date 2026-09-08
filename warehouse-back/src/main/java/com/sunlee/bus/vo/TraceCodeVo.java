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
}
