package com.sayai.record.kbo.model;

import com.sayai.record.model.Player;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GAME_IDX")
    private KboGame game;

    @Column(name = "CLUB_IDX")
    private Long clubId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLAYER_ID")
    private Player player;

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

    // Additional fields like result if available
    private String result;
}
