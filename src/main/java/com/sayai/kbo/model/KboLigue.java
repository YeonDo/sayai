package com.sayai.kbo.model;

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
@Table(name = "kbo_lig")
public class KboLigue {

    @Id
    @Column(name = "league_id")
    private Long id;

    @Column(name = "season")
    private Long season;

}
