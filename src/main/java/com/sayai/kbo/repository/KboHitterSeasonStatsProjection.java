package com.sayai.kbo.repository;

public interface KboHitterSeasonStatsProjection {
    Long getId();
    String getName();
    String getTeam();
    Integer getPa();
    Integer getAb();
    Integer getHit();
    String getAvg();
    Integer getHr();
    Integer getRbi();
    Integer getSo();
    Integer getSb();
}
