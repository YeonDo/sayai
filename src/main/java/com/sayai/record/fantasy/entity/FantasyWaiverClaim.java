package com.sayai.record.fantasy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "ft_waiver")
@Entity
@IdClass(FantasyWaiverClaimId.class)
public class FantasyWaiverClaim implements Persistable<FantasyWaiverClaimId> {

    @Id
    @Column(name = "waiver_seq", nullable = false)
    private Long waiverSeq;

    @Id
    @Column(name = "claim_player_id", nullable = false)
    private Long claimPlayerId;

    @Override
    @Transient
    public FantasyWaiverClaimId getId() {
        return new FantasyWaiverClaimId(waiverSeq, claimPlayerId);
    }

    @Override
    @Transient
    public boolean isNew() {
        return true;
    }

}
