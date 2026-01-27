package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.FantasyParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FantasyParticipantRepository extends JpaRepository<FantasyParticipant, Long> {
    Optional<FantasyParticipant> findByFantasyGameSeqAndPlayerId(Long fantasyGameSeq, Long playerId);
    List<FantasyParticipant> findByFantasyGameSeq(Long fantasyGameSeq);
}
