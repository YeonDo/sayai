package com.sayai.record.fantasy.entity;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ft_logs")
@Entity
public class FantasyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    private Long fantasyGameSeq;

    private Long playerId;

    private Long fantasyPlayerSeq;

    @Enumerated(EnumType.STRING)
    private ActionType action;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum ActionType {
        DRAFT,
        DROP,
        CLAIM,
        ADMIN_ASSIGN
    }
}
