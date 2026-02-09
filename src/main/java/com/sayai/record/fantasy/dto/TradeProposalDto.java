package com.sayai.record.fantasy.dto;

import lombok.Data;
import java.util.List;

@Data
public class TradeProposalDto {
    private Long gameSeq;
    private Long targetPlayerId;
    private List<Long> myPlayers; // List of FantasyPlayerSeqs
    private List<Long> targetPlayers; // List of FantasyPlayerSeqs
}
