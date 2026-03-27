package com.sayai.kbo.repository;

public interface KboPitchStatInterface {
    Long getId();
    Long getBackNo();
    String getName();
    Long getWins();
    Long getLoses();
    Long getSaves();
    Long getInning();
    Long getBatter();
    Long getBaseOnBall(); // bb
    Long getHitByBall(); // hbp
    Long getPHit(); // hit
    Long getSelfLossScore(); // er
    Long getPitchCnt(); // pitch_cnt
    Long getStOut(); // so
}
