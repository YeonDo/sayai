package com.sayai.record.fantasy.entity;

import lombok.*;

import jakarta.persistence.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ft_players")
@Entity
public class FantasyPlayer {

    @Id
    private Long seq;

    private String name;

    private String position;

    private String team;

    private String stats;

    private Integer cost;

    @Enumerated(EnumType.STRING)
    private ForeignerType foreignerType;

    public enum ForeignerType {
        TYPE_1, // Foreigner
        TYPE_2, // Asian Quarter
        NONE    // Domestic
    }
}
