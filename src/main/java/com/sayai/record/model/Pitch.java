package com.sayai.record.model;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "PITCH")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Builder
public class Pitch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PITCH_ID")
    private Long id;
    @Column(name = "GAME_IDX")
    private Long gameId;
    @Column(name = "CLUB_IDX")
    private Long clubId;

    private Long playerId;

    private String result;

    private Long inning;

    private Long batter;

    private Long hitter;

    private Long pHit;

    private Long pHomerun;

    private Long sacrifice;

    private Long sacFly;

    private Long baseOnBall;

    private Long hitByBall;

    private Long stOut;

    private Long fallingBall;

    private Long balk;

    private Long lossScore;

    private Long selfLossScore;

    private Long pitchCnt;

}
