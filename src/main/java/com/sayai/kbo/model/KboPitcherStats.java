package com.sayai.kbo.model;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name = "kbo_pitcher_stats")
@IdClass(KboPitcherStatsId.class)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class KboPitcherStats {

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Id
    @Column(name = "season")
    private Integer season;

    @Column(name = "outs")
    private Integer outs;

    @Column(name = "er")
    private Integer er;

    @Column(name = "era")
    private String era;

    @Column(name = "win")
    private Integer win;

    @Column(name = "so")
    private Integer so;

    @Column(name = "save")
    private Integer save;

    @Column(name = "bb")
    private Integer bb;

    @Column(name = "phit")
    private Integer phit;

    @Column(name = "whip")
    private String whip;
}
