package com.sunlee.bus.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * 档案主键：客户 1xxxxx、供应商 2xxxxx、药品 3xxxxx、器械 4xxxxx。
 */
public final class PharmaIds {

    public static final long CUSTOMER_START = 100001L;
    public static final long CUSTOMER_END = 199999L;
    public static final long SUPPLIER_START = 200001L;
    public static final long SUPPLIER_END = 299999L;
    public static final long DRUG_START = 300001L;
    public static final long DRUG_END = 399999L;
    public static final long DEVICE_START = 400001L;
    public static final long DEVICE_END = 499999L;

    public static final String CATEGORY_DRUG = "药品";
    public static final String CATEGORY_DEVICE = "器械";

    private PharmaIds() {
    }

    public static String categoryOf(Long id) {
        if (id != null && id >= DEVICE_START && id <= DEVICE_END) {
            return CATEGORY_DEVICE;
        }
        return CATEGORY_DRUG;
    }

    public static long[] rangeOfCategory(String category) {
        if (CATEGORY_DEVICE.equals(category)) {
            return new long[]{DEVICE_START, DEVICE_END};
        }
        return new long[]{DRUG_START, DRUG_END};
    }

    public static <T> Long allocate(BaseMapper<T> mapper, long start, long end) {
        QueryWrapper<T> qw = new QueryWrapper<>();
        qw.select("MAX(id)");
        qw.ge("id", start);
        qw.le("id", end);
        List<Object> objs = mapper.selectObjs(qw);
        Long max = null;
        if (objs != null && !objs.isEmpty() && objs.get(0) != null) {
            max = ((Number) objs.get(0)).longValue();
        }
        long next = (max == null || max < start) ? start : max + 1;
        if (next > end) {
            throw new IllegalStateException("编号区间已满: " + start + "-" + end);
        }
        return next;
    }
}
