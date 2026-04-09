package com.sayai.record.fantasy.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FantasyWaiverClaimId implements Serializable {
    private Long waiverSeq;
    private Long claimPlayerId;
}
