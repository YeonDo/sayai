package com.sayai.record.fantasy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ft_trade_board", uniqueConstraints = @UniqueConstraint(columnNames = {"trade_seq", "player_id"}))
@Entity
public class TradeBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(name = "trade_seq", nullable = false)
    private Long tradeSeq;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "vote_agree", nullable = false)
    private Boolean voteAgree;

    @Column(name = "voted_at", nullable = false)
    private LocalDateTime votedAt;
}
