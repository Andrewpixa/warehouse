package com.sunlee.bus.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PharmaAmounts {

    private PharmaAmounts() {
    }

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static BigDecimal lineAmount(BigDecimal qty, BigDecimal price) {
        return nz(qty).multiply(nz(price)).setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean moneyEquals(BigDecimal left, BigDecimal right) {
        return nz(left).compareTo(nz(right)) == 0;
    }
}
