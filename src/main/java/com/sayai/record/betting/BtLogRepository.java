package com.sayai.record.betting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BtLogRepository extends JpaRepository<BtLog, Long> {
    List<BtLog> findByBtGameId(Long btGameId);
    List<BtLog> findByBtGameIdIn(Collection<Long> btGameIds);
}
