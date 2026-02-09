package com.sayai.record.fantasy.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TradeLogDto {
    private Long tradeSeq;
    private String proposerTeam;
    private String targetTeam;
    private String proposerPlayers;
    private String targetPlayers;
    private String status;
    private LocalDateTime createdAt;
}
