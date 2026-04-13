package com.sayai.kbo.repository;

import com.sayai.kbo.model.KboHit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
            "WHERE g.game_idx BETWEEN :startIdx AND :endIdx " +
            "AND p.position NOT IN ('SP', 'RP', 'CL') " +
            "GROUP BY p.seq " +
            "ORDER BY totalHits DESC",
            countQuery = "SELECT COUNT(DISTINCT p.seq) FROM kbo_hit h JOIN ft_players p ON h.PLAYER_ID = p.seq JOIN kbo_game g ON h.game_idx = g.game_idx WHERE g.game_idx BETWEEN :startIdx AND :endIdx AND p.position NOT IN ('SP', 'RP', 'CL')",
            nativeQuery = true)
    Page<KboHitStatInterface> getPlayerByPeriod(@Param("startIdx") Long startIdx, @Param("endIdx") Long endIdx, Pageable pageable);

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
            "WHERE g.game_idx BETWEEN :startIdx AND :endIdx " +
            "AND p.seq = :id " +
            "AND p.position NOT IN ('SP', 'RP', 'CL') " +
            "GROUP BY p.seq", nativeQuery = true)
    Optional<KboHitStatInterface> getPlayerByPeriodAndId(@Param("startIdx") Long startIdx, @Param("endIdx") Long endIdx, @Param("id") Long id);

    List<KboHit> findByGameIdx(Long gameIdx);

    @Query(value = "SELECT " +
            " h.PLAYER_ID as playerId, " +
            " IFNULL(SUM(h.pa), 0) as pa, " +
            " IFNULL(SUM(h.ab), 0) as ab, " +
            " IFNULL(SUM(h.hit), 0) as hit, " +
            " IFNULL(SUM(h.rbi), 0) as rbi, " +
            " IFNULL(SUM(h.run), 0) as run, " +
            " IFNULL(SUM(h.sb), 0) as sb, " +
            " IFNULL(SUM(h.so), 0) as so, " +
            " IFNULL(SUM(h.hr), 0) as hr " +
            "FROM kbo_hit h " +
            "JOIN kbo_game g ON h.game_idx = g.game_idx " +
            "WHERE g.game_idx BETWEEN :startIdx AND :endIdx " +
            "AND h.PLAYER_ID IN (:playerIds) " +
            "GROUP BY h.PLAYER_ID", nativeQuery = true)
    List<KboParticipantStatsInterface> getAggregatedHitStats(@Param("startIdx") Long startIdx, @Param("endIdx") Long endIdx, @Param("playerIds") List<Long> playerIds);

    @Query(value = "SELECT " +
            " h.PLAYER_ID as playerId, " +
            " IFNULL(SUM(h.pa), 0) as pa, " +
            " IFNULL(SUM(h.ab), 0) as ab, " +
            " IFNULL(SUM(h.hit), 0) as hit, " +
            " IFNULL(SUM(h.rbi), 0) as rbi, " +
            " IFNULL(SUM(h.sb), 0) as sb, " +
            " IFNULL(SUM(h.so), 0) as so, " +
            " IFNULL(SUM(h.hr), 0) as hr " +
            "FROM kbo_hit h " +
            "JOIN kbo_game g ON h.game_idx = g.game_idx " +
            "WHERE h.PLAYER_ID = :playerId " +
            "AND g.game_idx BETWEEN :startIdx AND :endIdx", nativeQuery = true)
    KboHitterSeasonStatInterface getSeasonStatsByPlayerId(@Param("playerId") Long playerId, @Param("startIdx") Long startIdx, @Param("endIdx") Long endIdx);
}
