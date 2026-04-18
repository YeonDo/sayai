package com.sayai.kbo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HitterDailyStatDto {
    private String gameDate;   // yyyyMMdd
    private String opponent;
    private Long pa;
    private Long ab;
    private Long hit;
    private Long hr;
    private Long rbi;
    private Long run;
    private Long sb;
    private Long so;
    private Double battingAvg;
}
