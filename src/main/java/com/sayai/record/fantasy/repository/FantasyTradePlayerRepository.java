package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.FantasyTradePlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FantasyTradePlayerRepository extends JpaRepository<FantasyTradePlayer, Long> {
    List<FantasyTradePlayer> findByFantasyTradeSeq(Long fantasyTradeSeq);
}
