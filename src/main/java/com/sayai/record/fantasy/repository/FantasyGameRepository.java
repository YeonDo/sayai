package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.FantasyGame;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FantasyGameRepository extends JpaRepository<FantasyGame, Long> {
}
