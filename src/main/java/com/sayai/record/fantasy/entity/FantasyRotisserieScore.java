package com.sayai.record.fantasy.entity;

import lombok.*;

import jakarta.persistence.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ft_score_rotisserie")
@Entity
public class FantasyRotisserieScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(name = "fantasy_game_seq", nullable = false)
    private Long fantasyGameSeq;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(nullable = false)
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

    // Ranks (1 is best)
    private Integer rankAvg;
    private Integer rankRbi;
    private Integer rankHr;
    private Integer rankSoBatter;
    private Integer rankSb;

    private Integer rankWins;
    private Integer rankEra;
    private Integer rankSoPitcher;
    private Integer rankWhip;
    private Integer rankSaves;

    // Points (Calculated from Ranks)
    private Double pointsAvg;
    private Double pointsRbi;
    private Double pointsHr;
    private Double pointsSoBatter;
    private Double pointsSb;

    private Double pointsWins;
    private Double pointsEra;
    private Double pointsSoPitcher;
    private Double pointsWhip;
    private Double pointsSaves;

    private Double totalPoints;
}
