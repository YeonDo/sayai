package com.sayai.record.fantasy.entity;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
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

    private Integer maxParticipants;
    private LocalDateTime draftDate;

    private Integer draftTimeLimit; // Minutes. 0 = No limit.

    private LocalDateTime nextPickDeadline;

    private String gameDuration;

    private LocalDateTime createdAt;

    public void setStatus(GameStatus status) {
        this.status = status;
    }

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
        if (this.draftTimeLimit == null) {
            this.draftTimeLimit = 10;
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
