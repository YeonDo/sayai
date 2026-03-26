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
            " p.PLAYER_ID as id, p.BACK_NO as backNo, p.NAME as name, " +
            " COUNT(DISTINCT h.GAME_IDX) as totalGames, " +
            " IFNULL(SUM(h.pa), 0) as playerAppearance, " +
            " IFNULL(SUM(h.ab), 0) as atBat, " +
            " IFNULL(SUM(h.hit), 0) as totalHits, " +
            " IFNULL(SUM(h.so), 0) as strikeOut, " +
            " IFNULL(SUM(h.hr), 0) as homeruns, " +
            " IFNULL(SUM(h.rbi), 0) as rbi, " +
            " IFNULL(SUM(h.run), 0) as runs, " +
            " IFNULL(SUM(h.sb), 0) as sb " +
            "FROM kbo_hit h " +
            "JOIN player p ON h.PLAYER_ID = p.PLAYER_ID " +
            "JOIN kbo_game g ON h.GAME_IDX = g.GAME_IDX " +
            "WHERE g.game_date BETWEEN :startDate AND :endDate " +
            "AND p.sleep_yn = 'N' " +
            "GROUP BY p.PLAYER_ID " +
            "ORDER BY totalHits DESC", nativeQuery = true)
    List<KboHitStatInterface> getPlayerByPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT " +
            " p.PLAYER_ID as id, p.BACK_NO as backNo, p.NAME as name, " +
            " COUNT(DISTINCT h.GAME_IDX) as totalGames, " +
            " IFNULL(SUM(h.pa), 0) as playerAppearance, " +
            " IFNULL(SUM(h.ab), 0) as atBat, " +
            " IFNULL(SUM(h.hit), 0) as totalHits, " +
            " IFNULL(SUM(h.so), 0) as strikeOut, " +
            " IFNULL(SUM(h.hr), 0) as homeruns, " +
            " IFNULL(SUM(h.rbi), 0) as rbi, " +
            " IFNULL(SUM(h.run), 0) as runs, " +
            " IFNULL(SUM(h.sb), 0) as sb " +
            "FROM kbo_hit h " +
            "JOIN player p ON h.PLAYER_ID = p.PLAYER_ID " +
            "JOIN kbo_game g ON h.GAME_IDX = g.GAME_IDX " +
            "WHERE g.game_date BETWEEN :startDate AND :endDate " +
            "AND p.PLAYER_ID = :id " +
            "AND p.sleep_yn = 'N' " +
            "GROUP BY p.PLAYER_ID", nativeQuery = true)
    Optional<KboHitStatInterface> getPlayerByPeriodAndId(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("id") Long id);
}
