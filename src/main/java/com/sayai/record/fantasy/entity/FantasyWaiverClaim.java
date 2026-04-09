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
@IdClass(FantasyWaiverClaimId.class)
public class FantasyWaiverClaim {

    @Id
    @Column(name = "waiver_seq", nullable = false)
    private Long waiverSeq; // Corresponds to RosterTransaction seq

    @Id
    @Column(name = "claim_player_id", nullable = false)
    private Long claimPlayerId;

}
