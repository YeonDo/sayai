package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.FantasyTrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FantasyTradeRepository extends JpaRepository<FantasyTrade, Long> {
    List<FantasyTrade> findByFantasyGameSeqAndStatus(Long fantasyGameSeq, FantasyTrade.TradeStatus status);
}
