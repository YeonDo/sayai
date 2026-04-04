package com.sayai.kbo.repository;

public interface KboParticipantStatsInterface {
    Long getPlayerId(); // Maps to ft_players seq

    // Hitters
    Long getPa();
    Long getAb();
    Long getHit();
    Long getRbi();
    Long getRun();
    Long getSb();
    Long getSo();
    Long getHr();

    // Pitchers
    Long getWin();
    Long getLose();
    Long getSave();
    Long getInning();
    Long getBatter();
    Long getPitchCnt();
    Long getPHit();
    Long getBb();
    Long getPSo();
    Long getEr();
    Long getHbp();
}
