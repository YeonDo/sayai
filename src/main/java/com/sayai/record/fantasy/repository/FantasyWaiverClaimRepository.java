package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.FantasyWaiverClaim;
import com.sayai.record.fantasy.entity.FantasyWaiverClaimId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FantasyWaiverClaimRepository extends JpaRepository<FantasyWaiverClaim, FantasyWaiverClaimId> {
    List<FantasyWaiverClaim> findByWaiverSeq(Long waiverSeq);
    List<FantasyWaiverClaim> findByWaiverSeqIn(List<Long> waiverSeqs);
}
