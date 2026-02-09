package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.FantasyLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FantasyLogRepository extends JpaRepository<FantasyLog, Long> {
    List<FantasyLog> findByFantasyGameSeqOrderByCreatedAtAsc(Long fantasyGameSeq);
    List<FantasyLog> findByFantasyGameSeqAndActionOrderByCreatedAtDesc(Long fantasyGameSeq, FantasyLog.ActionType action);
}
