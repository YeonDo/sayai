package com.sayai.record.fantasy.entity;

import lombok.*;

import jakarta.persistence.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ft_players")
@Entity
public class FantasyPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    private String name;

    private String position;

    private String team;

    private String stats;

    private Integer cost;
}
