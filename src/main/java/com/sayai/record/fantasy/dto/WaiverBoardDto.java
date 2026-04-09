package com.sayai.record.fantasy.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class WaiverBoardDto {
    private Long transactionSeq;
    private Long requesterId;
    private String requesterTeamName;
    private String playerName;
    private String playerTeam;
    private String playerPosition;
    private Integer playerCost;
    private LocalDateTime waiverDate;
    private List<Long> claimPlayerIds; // If currently claimed by someone
}
