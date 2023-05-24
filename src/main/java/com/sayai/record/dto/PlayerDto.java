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
    private String battingAvg;
    private Long playerAppearance;
    private Long atBat;
    private Long totalHits;
    private Long singles;
    private Long doubles;
    private Long triples;
    private Long homeruns;
}
