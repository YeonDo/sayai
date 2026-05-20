package com.sayai.kbo.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MyRosterStatDto {

    private List<HitterStat> hitters;
    private List<PitcherStat> pitchers;
    private HitterStat hitterTotal;
    private PitcherStat pitcherTotal;

    @Getter
    @Builder
    public static class HitterStat {
        private Long fantasyPlayerSeq;
        private String playerName;
        private String kboTeam;
        private String assignedPosition;
        private long pa;
        private long ab;
        private long hit;
        private long hr;
        private long rbi;
        private long run;
        private long sb;
        private long so;
        private String avg;
    }

    @Getter
    @Builder
    public static class PitcherStat {
        private Long fantasyPlayerSeq;
        private String playerName;
        private String kboTeam;
        private String assignedPosition;
        private long win;
        private long lose;
        private long save;
        private long inning;
        private String formattedInning;
        private long er;
        private long bb;
        private long pHit;
        private long pSo;
        private long hbp;
        private String era;
        private String whip;
    }
}
