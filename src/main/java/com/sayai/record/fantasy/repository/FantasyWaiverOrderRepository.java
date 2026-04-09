package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.FantasyWaiverOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import java.util.List;

public interface FantasyWaiverOrderRepository extends JpaRepository<FantasyWaiverOrder, Long> {
    Optional<FantasyWaiverOrder> findByGameSeqAndPlayerId(Long gameSeq, Long playerId);

    List<FantasyWaiverOrder> findByGameSeqOrderByOrderNumAsc(Long gameSeq);

    @Query("SELECT MAX(w.orderNum) FROM FantasyWaiverOrder w WHERE w.gameSeq = :gameSeq")
    Integer findMaxOrderNumByGameSeq(@Param("gameSeq") Long gameSeq);
}
