package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.FantasyGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FantasyGameRepository extends JpaRepository<FantasyGame, Long> {

    @Query("SELECT g FROM FantasyGame g WHERE g.status = :status AND g.draftTimeLimit > 0 AND g.nextPickDeadline < :now")
    List<FantasyGame> findExpiredDraftingGames(@Param("status") FantasyGame.GameStatus status, @Param("now") LocalDateTime now);
}
