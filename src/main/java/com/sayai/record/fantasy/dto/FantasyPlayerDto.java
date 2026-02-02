package com.sayai.record.fantasy.dto;

import com.sayai.record.fantasy.entity.FantasyPlayer;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FantasyPlayerDto {
    private Long seq;
    private String name;
    private String position;
    private String team;
    private String stats;
    private Integer cost;

    public static FantasyPlayerDto from(FantasyPlayer entity) {
        return FantasyPlayerDto.builder()
                .seq(entity.getSeq())
                .name(entity.getName())
                .position(entity.getPosition())
                .team(entity.getTeam())
                .stats(entity.getStats())
                .cost(entity.getCost())
                .build();
    }
}
