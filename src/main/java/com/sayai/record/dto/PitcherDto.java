package com.sayai.record.dto;

import lombok.*;

import java.util.zip.DeflaterOutputStream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PitcherDto {
    private Long id;
    private Long backNo;
    private String name;
    private Long wins;
    private Long loses;
    private Long saves;
    private Long inn;
    private Double innings;
    private Long batter;
    private Long hitter;
    private Long pHit;
    private Long pHomerun;
    private Long sacrifice;
    private Long sacFly;
    private Long baseOnBall;
    private Long hitByBall;
    private Long stOut;
    private Long fallingBall;
    private Long balk;
    private Long lossScore;
    private Long selfLossScore;
    private Double era;
    private Double whip;
    private Double battingAvg;
    private Double k9;
    @Builder
    public PitcherDto(Long id, Long backNo, String name, Long wins, Long loses, Long saves, Long inn, Long batter, Long hitter, Long pHit, Long pHomerun, Long sacrifice, Long sacFly, Long baseOnBall, Long hitByBall, Long stOut, Long fallingBall, Long balk, Long lossScore, Long selfLossScore) {
        this.id = id;
        this.backNo = backNo;
        this.name = name;
        this.wins = wins;
        this.loses = loses;
        this.saves = saves;
        this.inn = inn;
        this.batter = batter;
        this.hitter = hitter;
        this.pHit = pHit;
        this.pHomerun = pHomerun;
        this.sacrifice = sacrifice;
        this.sacFly = sacFly;
        this.baseOnBall = baseOnBall;
        this.hitByBall = hitByBall;
        this.stOut = stOut;
        this.fallingBall = fallingBall;
        this.balk = balk;
        this.lossScore = lossScore;
        this.selfLossScore = selfLossScore;
    }
}
