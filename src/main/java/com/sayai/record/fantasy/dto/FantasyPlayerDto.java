package com.sayai.record.fantasy.dto;

import com.sayai.record.fantasy.entity.FantasyPlayer;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class FantasyPlayerDto {
    private Long seq;
    private String name;
    private String position;
    private String team;
    private String stats;
    private Integer cost;
    private String foreignerType;
    @Setter
    private String assignedPosition;

    public static FantasyPlayerDto from(FantasyPlayer entity) {
        return FantasyPlayerDto.builder()
                .seq(entity.getSeq())
                .name(entity.getName())
                .position(entity.getPosition())
                .team(entity.getTeam())
                .stats(entity.getStats())
                .cost(entity.getCost())
                .foreignerType(entity.getForeignerType() != null ? entity.getForeignerType().name() : "NONE")
                .build();
    }
}
