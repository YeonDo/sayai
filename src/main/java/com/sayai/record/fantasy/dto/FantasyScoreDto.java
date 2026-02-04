package com.sayai.record.fantasy.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FantasyScoreDto {
    private Long seq;
    private Long fantasyGameSeq;
    private Long playerId;
    private Integer round;

    // Batter Stats
    private Double avg;
    private Integer rbi;
    private Integer hr;
    private Integer soBatter; // K_batter
    private Integer sb;

    // Pitcher Stats
    private Integer wins;
    private Double era;
    private Integer soPitcher; // K_pitcher
    private Double whip;
    private Integer saves;

    // Points
    private Double totalPoints;
}
