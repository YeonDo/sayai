package com.sayai.record.fantasy.entity;

import lombok.*;

import jakarta.persistence.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ft_participants")
@Entity
public class FantasyParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(name = "fantasy_game_seq")
    private Long fantasyGameSeq;

    private Long playerId;

    private String teamName;

    private String preferredTeam;

    private Integer draftOrder;

    private Integer waiverOrder;
}
