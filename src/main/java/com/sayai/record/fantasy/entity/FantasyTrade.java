package com.sayai.record.fantasy.entity;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ft_trades")
@Entity
public class FantasyTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    private Long fantasyGameSeq;

    private Long proposerId;

    private Long targetId;

    @Enumerated(EnumType.STRING)
    private TradeStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TradeStatus.PROPOSED;
        }
    }

    public enum TradeStatus {
        PROPOSED,
        COMPLETED,
        REJECTED,
        CANCELED
    }
}
