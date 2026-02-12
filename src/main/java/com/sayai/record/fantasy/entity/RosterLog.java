package com.sayai.record.fantasy.entity;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ft_roster_log")
@Entity
public class RosterLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(name = "fantasy_game_seq")
    private Long fantasyGameSeq;

    @Column(name = "participant_id")
    private Long participantId;

    @Column(name = "fantasy_player_seq")
    private Long fantasyPlayerSeq;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    private LogActionType actionType;

    private LocalDateTime timestamp;

    private String details;

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now(ZoneId.of("UTC"));
    }

    public enum LogActionType {
        DRAFT_PICK,
        WAIVER_RELEASE,
        WAIVER_CLAIM,
        WAIVER_FA,
        TRADE_REQ,
        TRADE_SUCCESS,
        TRADE_REJECT,
        FA_ADD
    }
}
