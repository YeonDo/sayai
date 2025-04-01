package com.sayai.record.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {
    Utils utils = new Utils();
    @Test
    void getLastDayOfMonth() {
        assertEquals(utils.getLastDayOfMonth(2020,02),29);
        assertEquals(utils.getLastDayOfMonth(2022,02),28);
    }

}