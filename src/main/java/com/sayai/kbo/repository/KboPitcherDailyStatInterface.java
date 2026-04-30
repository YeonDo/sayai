package com.sayai.kbo.repository;

public interface KboPitcherDailyStatInterface {
    String getGameDate();   // yyyyMMdd
    String getOpponent();
    Long getInning();
    Long getWin();
    Long getLose();
    Long getSave();
    Long getEr();
    Long getBb();
    Long getHbp();
    Long getPHit();
    Long getSo();
    Long getPitchCnt();
    Long getBatter();
}
