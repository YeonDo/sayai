package com.sayai.record.kbo.repository;

import com.sayai.record.kbo.model.KboHit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface KboHitRepository extends JpaRepository<KboHit, Long> {

    @Query(value = "SELECT " +
            " p.seq as id, null as backNo, p.name as name, " +
            " COUNT(DISTINCT h.game_idx) as totalGames, " +
            " IFNULL(SUM(h.pa), 0) as playerAppearance, " +
            " IFNULL(SUM(h.ab), 0) as atBat, " +
            " IFNULL(SUM(h.hit), 0) as totalHits, " +
            " IFNULL(SUM(h.so), 0) as strikeOut, " +
            " IFNULL(SUM(h.hr), 0) as homeruns, " +
            " IFNULL(SUM(h.rbi), 0) as rbi, " +
            " IFNULL(SUM(h.run), 0) as runs, " +
            " IFNULL(SUM(h.sb), 0) as sb " +
            "FROM kbo_hit h " +
            "JOIN ft_players p ON h.PLAYER_ID = p.seq " +
            "JOIN kbo_game g ON h.game_idx = g.game_idx " +
            "WHERE g.season BETWEEN YEAR(:startDate) AND YEAR(:endDate) " +
            "GROUP BY p.seq " +
            "ORDER BY totalHits DESC", nativeQuery = true)
    List<KboHitStatInterface> getPlayerByPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT " +
            " p.seq as id, null as backNo, p.name as name, " +
            " COUNT(DISTINCT h.game_idx) as totalGames, " +
            " IFNULL(SUM(h.pa), 0) as playerAppearance, " +
            " IFNULL(SUM(h.ab), 0) as atBat, " +
            " IFNULL(SUM(h.hit), 0) as totalHits, " +
            " IFNULL(SUM(h.so), 0) as strikeOut, " +
            " IFNULL(SUM(h.hr), 0) as homeruns, " +
            " IFNULL(SUM(h.rbi), 0) as rbi, " +
            " IFNULL(SUM(h.run), 0) as runs, " +
            " IFNULL(SUM(h.sb), 0) as sb " +
            "FROM kbo_hit h " +
            "JOIN ft_players p ON h.PLAYER_ID = p.seq " +
            "JOIN kbo_game g ON h.game_idx = g.game_idx " +
            "WHERE g.season BETWEEN YEAR(:startDate) AND YEAR(:endDate) " +
            "AND p.seq = :id " +
            "GROUP BY p.seq", nativeQuery = true)
    Optional<KboHitStatInterface> getPlayerByPeriodAndId(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("id") Long id);
}
