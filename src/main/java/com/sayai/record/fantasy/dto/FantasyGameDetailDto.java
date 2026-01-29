package com.sayai.record.fantasy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FantasyGameDetailDto {
    private Long seq;
    private String title;
    private String ruleType;
    private String scoringType;
    private String scoringSettings;
    private String status;
    private String gameDuration;
    private Integer participantCount;
    private Integer maxParticipants;
    private Long nextPickerId;
    private LocalDateTime nextPickDeadline;
    private List<ParticipantRosterDto> participants;
}
