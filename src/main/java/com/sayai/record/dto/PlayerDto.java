package com.sayai.record.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class PlayerDto {
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
    @Builder
    public PlayerDto(Long id, Long backNo, String name, Long totalGames, Double battingAvg, Long playerAppearance, Double avgPa, Long atBat, Double onBasePer, Double slugPer, Long totalHits, Long singles, Long doubles, Long triples, Long homeruns) {
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
                '}';
    }
}
