package com.sunlee.sys.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 不启动 Spring、不连库。CI 里 {@code mvn -DskipTests=false test} 用。
 */
class ConstastTest {

    @Test
    void resultCodesMatchFrontContract() {
        assertEquals(Integer.valueOf(200), Constast.OK);
        assertEquals(Integer.valueOf(-1), Constast.ERROR);
    }

    @Test
    void menuTypeTokens() {
        assertEquals("menu", Constast.TYPE_MENU);
        assertEquals("permission", Constast.TYPE_PERMISSION);
    }
}
