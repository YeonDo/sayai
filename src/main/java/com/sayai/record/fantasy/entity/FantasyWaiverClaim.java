package com.sayai.record.fantasy.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ft_waiver")
@Entity
public class FantasyWaiverClaim {

    @Id
    @Column(name = "waiver_seq", nullable = false)
    private Long waiverSeq; // Corresponds to RosterTransaction seq

    @Column(name = "claim_player_id", nullable = false)
    private Long claimPlayerId;

    @Column(name = "claim_order", nullable = false)
    private Integer claimOrder;
}
