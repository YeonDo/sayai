package com.sayai.kbo.model;

import com.sayai.record.fantasy.entity.FantasyPlayer;
import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name = "kbo_hit")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class KboHit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HIT_ID")
    private Long id;


    @Column(name = "game_idx")
    private Long gameIdx;

    @Column(name = "PLAYER_ID")
    private Long playerId;

    @Column(name = "pa")
    private Long pa; // 타석

    @Column(name = "ab")
    private Long ab; // 타수

    @Column(name = "hit")
    private Long hit; // 안타

    @Column(name = "so")
    private Long so; // 삼진

    @Column(name = "hr")
    private Long hr; // 홈런

    @Column(name = "rbi")
    private Long rbi; // 타점

    @Column(name = "run")
    private Long run; // 득점

    @Column(name = "sb")
    private Long sb; // 도루
}
