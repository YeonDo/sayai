package com.sayai.record.kbo.repository;

public interface KboPitchStatInterface {
    Long getId();
    Long getBackNo();
    String getName();
    Long getInning();
    Long getBatter();
    Long getBaseOnBall(); // bb
    Long getHitByBall(); // hbp
    Long getPHit(); // hit
    Long getSelfLossScore(); // er
}
