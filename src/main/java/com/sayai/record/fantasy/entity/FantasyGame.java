package com.sayai.record.fantasy.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ft_games")
@Entity
public class FantasyGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    private String title;

    @Enumerated(EnumType.STRING)
    private GameStatus status;

    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    private ScoringType scoringType;

    @Column(columnDefinition = "TEXT")
    private String scoringSettings; // JSON format: {"AVG": 10, "HR": 50 ...}

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = GameStatus.WAITING;
        }
        if (this.ruleType == null) {
            this.ruleType = RuleType.RULE_1;
        }
        if (this.scoringType == null) {
            this.scoringType = ScoringType.POINTS; // Default
        }
    }

    public enum GameStatus {
        WAITING,
        DRAFTING,
        ONGOING,
        FINISHED
    }

    public enum RuleType {
        RULE_1,
        RULE_2
    }

    public enum ScoringType {
        POINTS,
        ROTISSERIE
    }
}
