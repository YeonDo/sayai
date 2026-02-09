package com.sayai.record.fantasy.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DraftLogDto {
    private Integer pickNumber;
    private String playerName;
    private String playerTeam;
    private String playerPosition;
    private String pickedByTeamName;
    private LocalDateTime pickedAt;
    private String action; // DRAFT, DROP, CLAIM
}
