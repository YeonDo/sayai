package com.sayai.record.fantasy.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DraftRequest {
    private Long fantasyGameSeq;
    private Long playerId;
    private Long fantasyPlayerSeq;
}
