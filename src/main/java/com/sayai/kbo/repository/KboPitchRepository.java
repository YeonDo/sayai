package com.sayai.kbo.repository;

import com.sayai.kbo.model.KboPitch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KboPitchRepository extends JpaRepository<KboPitch, Long> {

    @Query(value = "SELECT " +
            " p.seq as id, null as backNo, p.name as name, p.team as team, " +
            " IFNULL(SUM(pi.win), 0) as wins, " +
            " IFNULL(SUM(pi.lose), 0) as loses, " +
            " IFNULL(SUM(pi.save), 0) as saves, " +
            " IFNULL(SUM(pi.inning), 0) as inning, " +
            " IFNULL(SUM(pi.batter), 0) as batter, " +
            " IFNULL(SUM(pi.bb), 0) as baseOnBall, " +
            " IFNULL(SUM(pi.hbp), 0) as hitByBall, " +
            " IFNULL(SUM(pi.hit), 0) as pHit, " +
            " IFNULL(SUM(pi.er), 0) as selfLossScore, " +
            " IFNULL(SUM(pi.pitch_cnt), 0) as pitchCnt, " +
            " IFNULL(SUM(pi.so), 0) as stOut " +
            "FROM kbo_pitch pi " +
            "JOIN ft_players p ON pi.PLAYER_ID = p.seq " +
            "JOIN kbo_game g ON pi.game_idx = g.game_idx " +
            "WHERE g.game_idx BETWEEN :startIdx AND :endIdx " +
            "AND p.position IN ('SP', 'RP', 'CL') " +
            "GROUP BY p.seq " +
            "ORDER BY inning DESC",
            countQuery = "SELECT COUNT(DISTINCT p.seq) FROM kbo_pitch pi JOIN ft_players p ON pi.PLAYER_ID = p.seq JOIN kbo_game g ON pi.game_idx = g.game_idx WHERE g.game_idx BETWEEN :startIdx AND :endIdx AND p.position IN ('SP', 'RP', 'CL')",
            nativeQuery = true)
    Page<KboPitchStatInterface> getStatsByPeriod(@Param("startIdx") Long startIdx, @Param("endIdx") Long endIdx, Pageable pageable);

    @Query(value = "SELECT " +
            " p.seq as id, null as backNo, p.name as name, p.team as team, " +
            " IFNULL(SUM(pi.win), 0) as wins, " +
            " IFNULL(SUM(pi.lose), 0) as loses, " +
            " IFNULL(SUM(pi.save), 0) as saves, " +
            " IFNULL(SUM(pi.inning), 0) as inning, " +
            " IFNULL(SUM(pi.batter), 0) as batter, " +
            " IFNULL(SUM(pi.bb), 0) as baseOnBall, " +
            " IFNULL(SUM(pi.hbp), 0) as hitByBall, " +
            " IFNULL(SUM(pi.hit), 0) as pHit, " +
            " IFNULL(SUM(pi.er), 0) as selfLossScore, " +
            " IFNULL(SUM(pi.pitch_cnt), 0) as pitchCnt, " +
            " IFNULL(SUM(pi.so), 0) as stOut " +
            "FROM kbo_pitch pi " +
            "JOIN ft_players p ON pi.PLAYER_ID = p.seq " +
            "JOIN kbo_game g ON pi.game_idx = g.game_idx " +
            "WHERE g.game_idx BETWEEN :startIdx AND :endIdx " +
            "AND p.position IN :positions " +
            "GROUP BY p.seq " +
            "ORDER BY inning DESC",
            countQuery = "SELECT COUNT(DISTINCT p.seq) FROM kbo_pitch pi JOIN ft_players p ON pi.PLAYER_ID = p.seq JOIN kbo_game g ON pi.game_idx = g.game_idx WHERE g.game_idx BETWEEN :startIdx AND :endIdx AND p.position IN :positions",
            nativeQuery = true)
    Page<KboPitchStatInterface> getStatsByPeriodAndPositions(@Param("startIdx") Long startIdx, @Param("endIdx") Long endIdx, @Param("positions") List<String> positions, Pageable pageable);

    @Query(value = "SELECT " +
            " p.seq as id, null as backNo, p.name as name, p.team as team, " +
            " IFNULL(SUM(pi.win), 0) as wins, " +
            " IFNULL(SUM(pi.lose), 0) as loses, " +
            " IFNULL(SUM(pi.save), 0) as saves, " +
            " IFNULL(SUM(pi.inning), 0) as inning, " +
            " IFNULL(SUM(pi.batter), 0) as batter, " +
            " IFNULL(SUM(pi.bb), 0) as baseOnBall, " +
            " IFNULL(SUM(pi.hbp), 0) as hitByBall, " +
            " IFNULL(SUM(pi.hit), 0) as pHit, " +
            " IFNULL(SUM(pi.er), 0) as selfLossScore, " +
            " IFNULL(SUM(pi.pitch_cnt), 0) as pitchCnt, " +
            " IFNULL(SUM(pi.so), 0) as stOut " +
            "FROM kbo_pitch pi " +
            "JOIN ft_players p ON pi.PLAYER_ID = p.seq " +
            "JOIN kbo_game g ON pi.game_idx = g.game_idx " +
            "WHERE g.game_idx BETWEEN :startIdx AND :endIdx " +
            "AND p.seq = :id " +
            "AND p.position IN ('SP', 'RP', 'CL') " +
            "GROUP BY p.seq", nativeQuery = true)
    Optional<KboPitchStatInterface> getStatsByPeriodAndId(@Param("startIdx") Long startIdx, @Param("endIdx") Long endIdx, @Param("id") Long id);

    List<KboPitch> findByGameIdx(Long gameIdx);

    @Query(value = "SELECT " +
            " pi.PLAYER_ID as playerId, " +
            " IFNULL(SUM(pi.inning), 0) as outs, " +
            " IFNULL(SUM(pi.er), 0) as er, " +
            " IFNULL(SUM(pi.win), 0) as win, " +
            " IFNULL(SUM(pi.so), 0) as so, " +
            " IFNULL(SUM(pi.save), 0) as save, " +
            " IFNULL(SUM(pi.bb), 0) as bb, " +
            " IFNULL(SUM(pi.hit), 0) as phit " +
            "FROM kbo_pitch pi " +
            "JOIN kbo_game g ON pi.game_idx = g.game_idx " +
            "WHERE pi.PLAYER_ID = :playerId " +
            "AND g.game_idx BETWEEN :startIdx AND :endIdx", nativeQuery = true)
    KboPitcherSeasonStatInterface getSeasonStatsByPlayerId(@Param("playerId") Long playerId, @Param("startIdx") Long startIdx, @Param("endIdx") Long endIdx);

    @Query(value = "SELECT " +
            " pi.PLAYER_ID as playerId, " +
            " IFNULL(SUM(pi.win), 0) as win, " +
            " IFNULL(SUM(pi.lose), 0) as lose, " +
            " IFNULL(SUM(pi.save), 0) as save, " +
            " IFNULL(SUM(pi.inning), 0) as inning, " +
            " IFNULL(SUM(pi.batter), 0) as batter, " +
            " IFNULL(SUM(pi.pitch_cnt), 0) as pitchCnt, " +
            " IFNULL(SUM(pi.hit), 0) as pHit, " +
            " IFNULL(SUM(pi.bb), 0) as bb, " +
            " IFNULL(SUM(pi.so), 0) as pSo, " +
            " IFNULL(SUM(pi.er), 0) as er, " +
            " IFNULL(SUM(pi.hbp), 0) as hbp " +
            "FROM kbo_pitch pi " +
            "JOIN kbo_game g ON pi.game_idx = g.game_idx " +
            "WHERE g.game_idx BETWEEN :startIdx AND :endIdx " +
            "AND pi.PLAYER_ID IN (:playerIds) " +
            "GROUP BY pi.PLAYER_ID", nativeQuery = true)
    List<KboParticipantStatsInterface> getAggregatedPitchStats(@Param("startIdx") Long startIdx, @Param("endIdx") Long endIdx, @Param("playerIds") List<Long> playerIds);

    @Query(value = "SELECT " +
            " SUBSTRING(CAST(g.game_idx AS CHAR), 1, 8) as gameDate, " +
            " CASE WHEN g.home = p.team THEN g.away ELSE g.home END as opponent, " +
            " pi.inning as inning, pi.win as win, pi.lose as lose, pi.save as save, " +
            " pi.er as er, pi.bb as bb, pi.hbp as hbp, pi.hit as pHit, pi.so as so " +
            "FROM kbo_pitch pi " +
            "JOIN ft_players p ON pi.PLAYER_ID = p.seq " +
            "JOIN kbo_game g ON pi.game_idx = g.game_idx " +
            "WHERE pi.PLAYER_ID = :playerId " +
            "AND g.game_idx BETWEEN :startIdx AND :endIdx " +
            "ORDER BY g.game_idx DESC",
            countQuery = "SELECT COUNT(*) FROM kbo_pitch pi " +
            "JOIN kbo_game g ON pi.game_idx = g.game_idx " +
            "WHERE pi.PLAYER_ID = :playerId " +
            "AND g.game_idx BETWEEN :startIdx AND :endIdx",
            nativeQuery = true)
    Page<KboPitcherDailyStatInterface> getDailyStatsByPlayerId(
            @Param("playerId") Long playerId,
            @Param("startIdx") Long startIdx,
            @Param("endIdx") Long endIdx,
            Pageable pageable);
}
