package com.sayai.kbo.model;

import com.sayai.record.fantasy.entity.FantasyPlayer;
import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name = "kbo_pitch")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class KboPitch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PITCH_ID")
    private Long id;

    @Column(name = "game_idx")
    private Long gameIdx;

    @Column(name = "PLAYER_ID")
    private Long playerId;

    @Column(name = "inning")
    private Long inning; // 이닝

    @Column(name = "batter")
    private Long batter; // 상대타자수

    @Column(name = "bb")
    private Long bb; // 볼넷

    @Column(name = "hbp")
    private Long hbp; // 사구

    @Column(name = "hit")
    private Long hit; // 피안타

    @Column(name = "er")
    private Long er; // 자책점

    @Column(name = "win")
    private Long win; // 승리

    @Column(name = "lose")
    private Long lose; // 패배

    @Column(name = "save")
    private Long save; // 세이브

    @Column(name = "pitch_cnt")
    private Long pitchCnt; // 투구수

    @Column(name = "so")
    private Long so; // 탈삼진

}
