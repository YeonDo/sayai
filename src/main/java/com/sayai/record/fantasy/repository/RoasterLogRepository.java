package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.RoasterLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoasterLogRepository extends JpaRepository<RoasterLog, Long> {
    List<RoasterLog> findByFantasyGameSeqOrderByTimestampDesc(Long fantasyGameSeq);
}
