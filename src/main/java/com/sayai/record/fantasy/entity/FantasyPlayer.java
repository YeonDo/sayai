package com.sayai.record.fantasy.entity;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ft_players")
@Entity
public class FantasyPlayer {

    @Id
    private Long seq;

    private String name;

    private String position;

    private String team;

    private String stats;

    @NotNull(message = "Cost must be non-negative")
    @Min(value = 0, message = "Cost must be non-negative")
    @Column(columnDefinition = "integer check (cost >= 0)")
    private Integer cost;

    @Enumerated(EnumType.STRING)
    private ForeignerType foreignerType;

    @Builder
    public FantasyPlayer(Long seq, String name, String position, String team, String stats, Integer cost, ForeignerType foreignerType) {
        this.seq = seq;
        this.name = name;
        this.position = position;
        this.team = team;
        this.stats = stats;
        setCost(cost);
        this.foreignerType = foreignerType;
    }

    public void setCost(Integer cost) {
        if (cost == null || cost < 0) {
            throw new IllegalArgumentException("Cost must be non-negative");
        }
        this.cost = cost;
    }

    public enum ForeignerType {
        TYPE_1, // Foreigner
        TYPE_2, // Asian Quarter
        NONE    // Domestic
    }
}
