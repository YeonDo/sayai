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
@Table(name = "ft_draft_picks")
@Entity
public class DraftPick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    private Long playerId;

    @Column(name = "fantasy_player_seq")
    private Long fantasyPlayerSeq;

    @Column(name = "fantasy_game_seq")
    private Long fantasyGameSeq;

    @Column(name = "pick_number")
    private Integer pickNumber;

    private String assignedPosition;

    private LocalDateTime pickedAt;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'NORMAL'")
    @Builder.Default
    private PickStatus pickStatus = PickStatus.NORMAL;

    @PrePersist
    public void prePersist() {
        if (this.pickedAt == null) {
            this.pickedAt = LocalDateTime.now(ZoneId.of("UTC"));
        }
        if (this.pickStatus == null) {
            this.pickStatus = PickStatus.NORMAL;
        }
    }

    public enum PickStatus {
        NORMAL,
        WAIVER_REQ,
        TRADE_PENDING
    }
}
