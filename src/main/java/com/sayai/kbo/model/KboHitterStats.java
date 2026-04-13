package com.sayai.kbo.model;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name = "kbo_hitter_stats")
@IdClass(KboHitterStatsId.class)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class KboHitterStats {

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Id
    @Column(name = "season")
    private Integer season;

    @Column(name = "ab")
    private Integer ab;

    @Column(name = "pa")
    private Integer pa;

    @Column(name = "hit")
    private Integer hit;

    @Column(name = "avg")
    private String avg;

    @Column(name = "hr")
    private Integer hr;

    @Column(name = "rbi")
    private Integer rbi;

    @Column(name = "so")
    private Integer so;

    @Column(name = "sb")
    private Integer sb;
}
