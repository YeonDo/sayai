package com.sayai.record.model;


import lombok.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "LIG")
public class Ligue {

    @Id
    @Column(name = "league_id")
    private Long id;
    @Column(name = "LIG_IDX")
    private Long ligIdx;

    private Long clubId;

    private Long season;

    @Column(name = "LIG_NAME")
    private String name;
    @Column(name="LIG_NAME_SEC")
    private String nameSec;
    private String leagueInfo;
    private Long gameRule;
    private Long bucode;
    private Long jocode;


}
