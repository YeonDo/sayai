package com.sayai.record.admin.dto;

import com.sayai.record.fantasy.entity.RosterTransaction;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminRosterTransactionDto {
    private Long seq;
    private Long fantasyGameSeq;
    private Long requesterId;
    private Long targetId;
    private RosterTransaction.TransactionType type;
    private RosterTransaction.TransactionStatus status;
    private String givingPlayerSeqs;
    private String receivingPlayerSeqs;
    private String givingPlayerDetails;
    private String receivingPlayerDetails;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminRosterTransactionDto from(RosterTransaction entity, String givingDetails, String receivingDetails) {
        return AdminRosterTransactionDto.builder()
                .seq(entity.getSeq())
                .fantasyGameSeq(entity.getFantasyGameSeq())
                .requesterId(entity.getRequesterId())
                .targetId(entity.getTargetId())
                .type(entity.getType())
                .status(entity.getStatus())
                .givingPlayerSeqs(entity.getGivingPlayerSeqs())
                .receivingPlayerSeqs(entity.getReceivingPlayerSeqs())
                .givingPlayerDetails(givingDetails)
                .receivingPlayerDetails(receivingDetails)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
