package com.sayai.record.fantasy.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
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

    private LocalDateTime pickedAt;

    @PrePersist
    public void prePersist() {
        this.pickedAt = LocalDateTime.now();
    }
}
