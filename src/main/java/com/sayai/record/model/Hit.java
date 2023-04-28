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
    @Column(name = "GAME_IDX")
    private Long gameId;

    private Long gameSeq;

    private Long playerId;

    private Long inning;

    private Long hitNo;

    private Long hitSeq;

    private String hitCd;

    private String result;


}
