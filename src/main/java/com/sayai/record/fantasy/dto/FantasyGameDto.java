package com.sayai.record.fantasy.dto;

import com.sayai.record.fantasy.entity.FantasyGame;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FantasyGameDto {
    private Long seq;
    private String title;
    private FantasyGame.GameStatus status;
    private FantasyGame.RuleType ruleType;
    private FantasyGame.ScoringType scoringType;
    private String scoringSettings;
    private Integer maxParticipants;
    private LocalDateTime draftDate;
    private Integer draftTimeLimit;
    private String gameDuration;
    private Boolean useFirstPickRule;
    private Integer salaryCap;
    private Boolean useTeamRestriction;
    private LocalDateTime createdAt;

    // Additional fields for Dashboard
    private Integer participantCount;
    private boolean isJoined;
    private String myTeamName;

    public static FantasyGameDto from(FantasyGame game) {
        return FantasyGameDto.builder()
                .seq(game.getSeq())
                .title(game.getTitle())
                .status(game.getStatus())
                .ruleType(game.getRuleType())
                .scoringType(game.getScoringType())
                .scoringSettings(game.getScoringSettings())
                .maxParticipants(game.getMaxParticipants())
                .draftDate(game.getDraftDate())
                .draftTimeLimit(game.getDraftTimeLimit())
                .gameDuration(game.getGameDuration())
                .useFirstPickRule(game.getUseFirstPickRule())
                .salaryCap(game.getSalaryCap())
                .useTeamRestriction(game.getUseTeamRestriction())
                .createdAt(game.getCreatedAt())
                .build();
    }
}
