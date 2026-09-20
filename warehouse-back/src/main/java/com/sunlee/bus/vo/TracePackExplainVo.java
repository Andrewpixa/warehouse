package com.sunlee.bus.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TracePackExplainVo {

    private String inputCode;

    private String rootCode;

    private String rootLevel;

    private String drugName;

    private String batchNo;

    private String expireDate;

    private String spec;

    private List<String> bigCodes = new ArrayList<>();

    private List<String> midCodes = new ArrayList<>();

    private List<String> smallCodes = new ArrayList<>();

    private List<Move> inbounds = new ArrayList<>();

    private List<Move> outbounds = new ArrayList<>();

    @Data
    public static class Move {
        private String billType;
        private String orderNo;
        private String invoiceNo;
        private String partyName;
        private String warehouseName;
        private String bizDate;
        private String status;
        private String spdid;
        private String qty;
        private String drugName;
        private String batchNo;
    }
}
