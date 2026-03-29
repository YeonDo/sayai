package com.sayai.kbo.repository;

public interface KboHitStatInterface {
    Long getId();
    Long getBackNo();
    String getName();
    Long getTotalGames();
    Long getPlayerAppearance(); // pa
    Long getAtBat(); // ab
    Long getTotalHits(); // hit
    Long getStrikeOut(); // so
    Long getHomeruns(); // hr
    Integer getRbi(); // rbi
    Integer getRuns(); // run
    Integer getSb(); // sb
}
