package com.sunlee.bus.vo;

import com.sunlee.bus.entity.OpsFlowDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class OpsFlowDocVo extends OpsFlowDoc {

    private Integer page = 1;

    private Integer limit = 10;

    private String confirmAction;
}
