package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.FantasyPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FantasyPlayerRepository extends JpaRepository<FantasyPlayer, Long> {
}
