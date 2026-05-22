package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.DraftPick;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DraftPickRepository extends JpaRepository<DraftPick, Long> {
    List<DraftPick> findByFantasyGameSeq(Long fantasyGameSeq);
    List<DraftPick> findByFantasyGameSeqOrderByPickNumberAsc(Long fantasyGameSeq);
    List<DraftPick> findByMemberId(Long memberId);
    List<DraftPick> findByFantasyGameSeqAndMemberId(Long fantasyGameSeq, Long memberId);
    Optional<DraftPick> findByFantasyGameSeqAndFantasyPlayerSeq(Long fantasyGameSeq, Long fantasyPlayerSeq);
    boolean existsByFantasyGameSeqAndFantasyPlayerSeq(Long fantasyGameSeq, Long fantasyPlayerSeq);
    long countByFantasyGameSeq(Long fantasyGameSeq);
}
