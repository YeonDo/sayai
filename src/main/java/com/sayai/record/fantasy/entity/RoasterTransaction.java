package com.sayai.record.fantasy.entity;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ft_transactions")
@Entity
public class RoasterTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(name = "fantasy_game_seq")
    private Long fantasyGameSeq;

    @Column(name = "requester_id")
    private Long requesterId;

    @Column(name = "target_id")
    private Long targetId; // Nullable for Waiver

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "giving_player_seqs")
    private String givingPlayerSeqs; // Comma-separated IDs

    @Column(name = "receiving_player_seqs")
    private String receivingPlayerSeqs; // Comma-separated IDs (Nullable for Waiver)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum TransactionType {
        WAIVER,
        TRADE
    }

    public enum TransactionStatus {
        REQUESTED,
        APPROVED,
        REJECTED,
        FA_MOVED // For Waiver -> FA
    }
}
