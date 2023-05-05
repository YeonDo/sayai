package com.sayai.record.model;


import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "HIT")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Builder
public class Hit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HIT_ID")
    private Long id;

    private Long gameSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GAME_IDX")
    private Game game;

    @Column(name = "CLUB_IDX")
    private Long clubId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLAYER_ID")
    private Player player;

    private Long inning;

    private Long hitNo;

    private Long hitSeq;

    private String hitCd;

    private String result;


}
