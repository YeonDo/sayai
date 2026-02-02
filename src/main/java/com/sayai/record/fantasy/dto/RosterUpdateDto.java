package com.sayai.record.fantasy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RosterUpdateDto {
    private List<RosterEntry> entries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RosterEntry {
        private Long fantasyPlayerSeq;
        private String assignedPosition;
    }
}
