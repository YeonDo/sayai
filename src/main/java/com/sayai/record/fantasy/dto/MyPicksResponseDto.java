package com.sayai.record.fantasy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPicksResponseDto {
    private List<FantasyPlayerDto> picks;
    private Integer currentCost;
}
