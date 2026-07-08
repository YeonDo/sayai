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
    private Long memberId;
    private Integer round;

    // Batter Stats
    private Double avg;
    private Integer rbi;
    private Integer hr;
    private Integer soBatter; // K_batter
    private Integer sb;
    private Integer run; // 득점

    // Pitcher Stats
    private Integer wins;
    private Double era;
    private Integer soPitcher; // K_pitcher
    private Double whip;
    private Integer saves;
    private Integer hold; // 홀드

    // Points
    private Double totalPoints;
}
