package com.sayai.record.fantasy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantStatsDto {
    private Long participantId;
    private String teamName;
    private String ownerName; // Optional, maybe useful

    // Batting
    private Double battingAvg;
    private Long homeruns;
    private Integer rbi;
    private Long batterStrikeOuts;
    private Integer stolenBases;

    // Pitching
    private Long wins;
    private Long pitcherStrikeOuts;
    private Double era;
    private Double whip;
    private Long saves;
}
