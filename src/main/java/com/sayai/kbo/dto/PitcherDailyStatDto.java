package com.sayai.kbo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PitcherDailyStatDto {
    private String gameDate;   // yyyyMMdd
    private String opponent;
    private Double innings;
    private Long win;
    private Long lose;
    private Long save;
    private Long er;
    private Long bb;
    private Long hbp;
    private Long pHit;
    private Long so;
    private Double era;
    private Long pitchCnt;
    private Double k9;
    private Double bb9;
    private Double kbb;
    private Double pip;
    private Double ppa;
}
