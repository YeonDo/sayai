package com.sayai.record.fantasy.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DraftLogDto {
    private Integer pickNumber;
    private String playerName;
    private String playerTeam;
    private String playerPosition;
    private String pickedByTeamName;
    private LocalDateTime pickedAt;
}
