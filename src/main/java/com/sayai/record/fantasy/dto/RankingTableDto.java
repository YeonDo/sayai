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
public class RankingTableDto {
    private Long gameSeq;
    private String scoringType;
    private List<ParticipantStatsDto> rankings;
}
