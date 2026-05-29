package com.sayai.record.fantasy.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Builder
public class DraftEventDto {
    private String type; // "PICK" or "JOIN" or "STATUS" or "START"
    private Long fantasyGameSeq;
    private Long memberId;
    private Long fantasyPlayerSeq;
    private String playerName;
    private String playerTeam;
    private Integer pickNumber;
    private String message;

    private Boolean isBot;
    private Boolean nextPickerIsBot;

    // Next Pick Info
    private Long nextPickerId;
    private ZonedDateTime nextPickDeadline;
    private Integer round;
    private Integer pickInRound;

    // For START event
    private List<ParticipantRosterDto> draftOrder;
}
