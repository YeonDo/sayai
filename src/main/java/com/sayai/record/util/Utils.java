package com.sayai.record.util;

import java.time.LocalDate;

public class Utils {
    public int getLastDayOfMonth(int year, int month){
        LocalDate lastDayOfMonth = LocalDate.of(year,month,1).withDayOfMonth(LocalDate.of(year,month,1).lengthOfMonth());
        return lastDayOfMonth.getDayOfMonth();
    }


}
