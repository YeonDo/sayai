package com.sayai.record.fantasy.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RosterLogDto {
    private Long seq;
    private String playerName;
    private String playerTeam;
    private String playerPosition;
    private String participantName;
    private String actionType;
    private String details;
    private LocalDateTime timestamp;
}
