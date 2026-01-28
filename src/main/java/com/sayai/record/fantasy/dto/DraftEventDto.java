package com.sayai.record.fantasy.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DraftEventDto {
    private String type; // "PICK" or "JOIN" or "STATUS" or "START"
    private Long fantasyGameSeq;
    private Long playerId;
    private Long fantasyPlayerSeq;
    private String playerName;
    private String playerTeam;
    private Integer pickNumber;
    private String message;

    // Next Pick Info
    private Long nextPickerId;
    private LocalDateTime nextPickDeadline;
    private Integer round;
    private Integer pickInRound;

    // For START event
    private List<ParticipantRosterDto> draftOrder;
}
