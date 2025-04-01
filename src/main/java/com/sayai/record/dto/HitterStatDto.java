package com.sayai.record.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HitterStatDto {
    private Long playerId;
    private int rbi;
    private int runs;
    private int stolenBases;

    public HitterStatDto(Long playerId, Long rbi, Long runs, Long stolenBases) {
        this.playerId = playerId;
        this.rbi = rbi != null ? rbi.intValue() : 0;
        this.runs = runs != null ? runs.intValue() : 0;
        this.stolenBases = stolenBases != null ? stolenBases.intValue() : 0;
    }

    // Getters and setters (or use Lombok @Getter)
}
