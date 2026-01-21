package com.sayai.record.fantasy.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DraftEventDto {
    private String type; // "PICK" or "JOIN" or "STATUS"
    private Long fantasyGameSeq;
    private Long playerId;
    private Long fantasyPlayerSeq;
    private String playerName;
    private String playerTeam;
    private Integer pickNumber;
    private String message;
}
