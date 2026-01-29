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
public class ParticipantRosterDto {
    private Long participantId;
    private String teamName;
    private String preferredTeam;
    private Integer draftOrder;
    private List<FantasyPlayerDto> roster;
}
