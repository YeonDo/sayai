package com.sayai.record.fantasy.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TradeBoardDto {

    private Long transactionSeq;
    private String requesterTeamName;
    private String targetTeamName;
    private List<PlayerInfo> givingPlayers;
    private List<PlayerInfo> receivingPlayers;
    private String status;     // SUGGESTED(제안됨), REQUESTED(투표 중)
    private String comment;    // 트레이드 제안 메시지
    private int agreeCount;
    private int disagreeCount;
    private Boolean myVote;    // null=미투표, true=찬성, false=반대 (isParty=true면 항상 null)
    private boolean isParty;   // 내가 트레이드 당사자 여부
    private boolean canRespond; // 내가 수락/거절 가능한지 여부 (SUGGESTED이고 내가 target인 경우)
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class PlayerInfo {
        private String playerName;
        private String teamName;
    }
}
