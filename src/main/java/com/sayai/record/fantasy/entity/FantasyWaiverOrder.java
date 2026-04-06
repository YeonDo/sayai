package com.sayai.record.fantasy.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ft_games_waiver")
@Entity
public class FantasyWaiverOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(name = "game_seq", nullable = false)
    private Long gameSeq;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "order_num", nullable = false)
    private Integer orderNum;
}
