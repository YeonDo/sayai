package com.sayai.record.model;

import lombok.*;

import jakarta.persistence.*;
@Entity
@Table(name = "PITCH_BOARD")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
@Builder
public class PitcherBoard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ")
    private Long id;

    private Long gameSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GAME_IDX")
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLAYER_ID")
    @Setter
    private Player player;

    private Long inning;

    private Long hitNo;

    private String hitCd;

    private String result;
}
