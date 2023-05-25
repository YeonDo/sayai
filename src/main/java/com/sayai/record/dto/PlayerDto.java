package com.sayai.record.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
