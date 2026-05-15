package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.TradeBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeBoardRepository extends JpaRepository<TradeBoard, Long> {
    List<TradeBoard> findByTradeSeq(Long tradeSeq);
    Optional<TradeBoard> findByTradeSeqAndPlayerId(Long tradeSeq, Long playerId);
    List<TradeBoard> findByTradeSeqIn(List<Long> tradeSeqs);
}
