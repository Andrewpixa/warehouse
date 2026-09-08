package com.sunlee.bus.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PharmaStatsResult {

    private String startDate;
    private String endDate;
    private Summary summary = new Summary();
    private List<DayRow> days = new ArrayList<>();
    private List<DrugRow> drugs = new ArrayList<>();

    @Data
    public static class Summary {
        private long orderCount;
        private BigDecimal qty = BigDecimal.ZERO;
        private BigDecimal amount = BigDecimal.ZERO;
    }

    @Data
    public static class DayRow {
        private String bizDate;
        private long orderCount;
        private BigDecimal qty = BigDecimal.ZERO;
        private BigDecimal amount = BigDecimal.ZERO;
    }

    @Data
    public static class DrugRow {
        private Long drugId;
        private String drugName;
        private BigDecimal qty = BigDecimal.ZERO;
        private BigDecimal amount = BigDecimal.ZERO;
    }
}
