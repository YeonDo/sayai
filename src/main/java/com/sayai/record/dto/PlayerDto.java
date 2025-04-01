package com.sayai.record.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class PlayerDto {
    private String season;
    private Long id;
    private Long backNo;
    private String name;
    private Long totalGames;
    private Double battingAvg;
    private Long playerAppearance;
    private Double avgPa;
    private Long atBat;
    private Double onBasePer;
    private Double slugPer;
    private Long totalHits;
    private Long singles;
    private Long doubles;
    private Long triples;
    private Long homeruns;
    private Long baseOnBall;
    private Long hitByPitch;
    private Long strikeOut;
    private Long ibb;
    private Long dp;
    private Long sacrifice;
    private Long sacFly;
    private Integer rbi;
    private Integer runs;
    private Integer sb;

    @Builder
    public PlayerDto(Long id, Long backNo, String name, Long totalGames, Double battingAvg, Long playerAppearance, Double avgPa, Long atBat, Double onBasePer, Double slugPer, Long totalHits, Long singles, Long doubles, Long triples, Long homeruns,Long baseOnBall, Long hitByPitch, Long strikeOut, Long ibb, Long dp, Long sacrifice, Long sacFly, Integer rbi, Integer runs, Integer sb) {
        this.id = id;
        this.backNo = backNo;
        this.name = name;
        this.totalGames = totalGames;
        this.battingAvg = battingAvg ==null? 0:battingAvg;
        this.playerAppearance = playerAppearance;
        this.avgPa = avgPa == null?0: avgPa;
        this.atBat = atBat;
        this.onBasePer = onBasePer == null ? 0: onBasePer;
        this.slugPer = slugPer== null ? 0: slugPer;
        this.totalHits = totalHits;
        this.singles = singles;
        this.doubles = doubles;
        this.triples = triples;
        this.homeruns = homeruns;
        this.baseOnBall = baseOnBall;
        this.hitByPitch = hitByPitch;
        this.strikeOut = strikeOut;
        this.ibb = ibb;
        this.dp = dp;
        this.sacrifice =sacrifice;
        this.sacFly = sacFly;
        this.rbi = rbi;
        this.runs = runs;
        this.sb = sb;
    }


    @Override
    public String toString() {
        return "PlayerDto{" +
                "id=" + id +
                ", backNo=" + backNo +
                ", name='" + name + '\'' +
                ", totalGames=" + totalGames +
                ", battingAvg=" + battingAvg +
                ", playerAppearance=" + playerAppearance +
                ", avgPa=" + avgPa +
                ", atBat=" + atBat +
                ", onBasePer=" + onBasePer +
                ", slugPer=" + slugPer +
                ", totalHits=" + totalHits +
                ", singles=" + singles +
                ", doubles=" + doubles +
                ", triples=" + triples +
                ", homeruns=" + homeruns +
                ", baseOnBall=" + baseOnBall +
                ", hitByPitch=" + hitByPitch +
                ", strikeOut=" + strikeOut +
                ", ibb=" + ibb +
                ", dp=" + dp +
                ", sacrifice=" + sacrifice +
                ", sacFly=" + sacFly +
                '}';
    }

    public void setSeason(String season) {
        this.season = season;
    }
}
