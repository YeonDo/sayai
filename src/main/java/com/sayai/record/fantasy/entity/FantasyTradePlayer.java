package com.sayai.record.fantasy.entity;

import lombok.*;

import jakarta.persistence.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ft_trade_players")
@Entity
public class FantasyTradePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    private Long fantasyTradeSeq;

    private Long playerId; // Owner at time of proposal

    private Long fantasyPlayerSeq;
}
