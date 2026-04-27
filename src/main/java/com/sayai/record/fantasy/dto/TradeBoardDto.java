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
    private int agreeCount;
    private int disagreeCount;
    private Boolean myVote;   // null=미투표, true=찬성, false=반대 (isParty=true면 항상 null)
    private boolean isParty;  // 내가 트레이드 당사자 여부 (true면 투표 불가)
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class PlayerInfo {
        private String playerName;
        private String teamName;
    }
}
