package com.sayai.kbo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParticipantKboStatsDto {
    private Long participantSeq;
    private Long playerId; // User's ID
    private String teamName;

    // Hitter Aggregated Stats
    private long pa;
    private long ab;
    private long hit;
    private long rbi;
    private long run;
    private long sb;
    private long so;
    private long hr;

    // Pitcher Aggregated Stats
    private long win;
    private long lose;
    private long save;
    private long inning; // Store as outs directly, convert to formatted string if necessary
    private String formattedInning;
    private long batter;
    private long pitchCnt;
    private long pHit;
    private long bb;
    private long pSo;
    private long er;
    private long hbp;
}
