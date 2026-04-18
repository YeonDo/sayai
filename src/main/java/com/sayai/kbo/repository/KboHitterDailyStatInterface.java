package com.sayai.kbo.repository;

public interface KboHitterDailyStatInterface {
    String getGameDate();   // yyyyMMdd
    String getOpponent();
    Long getPa();
    Long getAb();
    Long getHit();
    Long getHr();
    Long getRbi();
    Long getRun();
    Long getSb();
    Long getSo();
}
