package com.sayai.kbo.repository;

public interface KboPitcherSeasonStatsProjection {
    Long getId();
    String getName();
    Integer getOuts();
    Integer getEr();
    String getEra();
    Integer getWin();
    Integer getSo();
    Integer getSave();
    Integer getBb();
    Integer getPhit();
    String getWhip();
}
