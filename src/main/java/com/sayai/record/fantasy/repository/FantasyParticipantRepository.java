package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.FantasyParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FantasyParticipantRepository extends JpaRepository<FantasyParticipant, Long> {
    Optional<FantasyParticipant> findByFantasyGameSeqAndMemberId(Long fantasyGameSeq, Long memberId);
    List<FantasyParticipant> findByFantasyGameSeqAndMemberIdIn(Long fantasyGameSeq, Collection<Long> memberIds);
    List<FantasyParticipant> findByFantasyGameSeq(Long fantasyGameSeq);
    List<FantasyParticipant> findByFantasyGameSeqIn(Collection<Long> fantasyGameSeqs);
    List<FantasyParticipant> findByMemberId(Long memberId);
}
