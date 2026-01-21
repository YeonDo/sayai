package com.sayai.record.model;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name = "HITTER_BOARD")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class HitterBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GAME_IDX")
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLAYER_ID")
    private Player player;

    private Long hitNo;

    @Column(name = "PLAYER_APPEARANCE")
    private Integer playerApp;

    private Integer hits;

    private Integer rbi;

    private Integer runs;

    @Column(name = "STOLEN_BASES")
    private Integer sb;
}
