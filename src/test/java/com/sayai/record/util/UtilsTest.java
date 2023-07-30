package com.sayai.record.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void getLastDayOfMonth() {
        Utils utils = new Utils();
        assertEquals(utils.getLastDayOfMonth(2020,02),29);
        assertEquals(utils.getLastDayOfMonth(2022,02),28);
    }
}