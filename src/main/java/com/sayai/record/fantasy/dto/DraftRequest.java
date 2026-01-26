package com.sayai.record.fantasy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftRequest {
    private Long fantasyGameSeq;
    private Long playerId;
    private Long fantasyPlayerSeq;
}
