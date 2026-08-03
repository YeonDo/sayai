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

    @Column(name = "is_active", columnDefinition = "int default 0")
    private Integer isActive = 0;

    @Builder
    public FantasyPlayer(Long seq, String name, String position, String team, String stats, Integer cost, ForeignerType foreignerType, Integer isActive) {
        this.seq = seq;
        this.name = name;
        this.position = position;
        this.team = team;
        this.stats = stats;
        setCost(cost);
        this.foreignerType = foreignerType;
        this.isActive = isActive != null ? isActive : 0;
    }

    public void setCost(Integer cost) {
        if (cost == null || cost < 0) {
            throw new IllegalArgumentException("Cost must be non-negative");
        }
        this.cost = cost;
    }

    /** 어드민 선수 정보 수정 — null이 아닌 필드만 반영(부분 업데이트). cost는 setCost로 검증. */
    public void update(String name, String position, String team, String stats,
                       Integer cost, ForeignerType foreignerType, Integer isActive) {
        if (name != null) this.name = name;
        if (position != null) this.position = position;
        if (team != null) this.team = team;
        if (stats != null) this.stats = stats;
        if (cost != null) setCost(cost);
        if (foreignerType != null) this.foreignerType = foreignerType;
        if (isActive != null) this.isActive = isActive;
    }

    public enum ForeignerType {
        TYPE_1, // Foreigner
        TYPE_2, // Asian Quarter
        NONE    // Domestic
    }
}
