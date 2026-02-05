package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.FantasyRotisserieScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FantasyRotisserieScoreRepository extends JpaRepository<FantasyRotisserieScore, Long> {
    List<FantasyRotisserieScore> findByFantasyGameSeq(Long fantasyGameSeq);
    List<FantasyRotisserieScore> findByFantasyGameSeqAndRound(Long fantasyGameSeq, Integer round);
    Optional<FantasyRotisserieScore> findByFantasyGameSeqAndPlayerIdAndRound(Long fantasyGameSeq, Long playerId, Integer round);
}
