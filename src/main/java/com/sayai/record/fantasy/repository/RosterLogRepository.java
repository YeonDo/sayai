package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.RosterLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RosterLogRepository extends JpaRepository<RosterLog, Long> {
    List<RosterLog> findByFantasyGameSeqOrderByTimestampDesc(Long fantasyGameSeq);
}
