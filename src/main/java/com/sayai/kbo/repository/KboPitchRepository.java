package com.sayai.kbo.repository;

import com.sayai.kbo.model.KboPitch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface KboPitchRepository extends JpaRepository<KboPitch, Long> {

    @Query(value = "SELECT " +
            " p.seq as id, null as backNo, p.name as name, " +
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
            "WHERE g.season BETWEEN YEAR(:startDate) AND YEAR(:endDate) " +
            "AND p.position IN ('SP', 'RP', 'CL') " +
            "GROUP BY p.seq " +
            "ORDER BY inning DESC",
            countQuery = "SELECT COUNT(DISTINCT p.seq) FROM kbo_pitch pi JOIN ft_players p ON pi.PLAYER_ID = p.seq JOIN kbo_game g ON pi.game_idx = g.game_idx WHERE g.season BETWEEN YEAR(:startDate) AND YEAR(:endDate) AND p.position IN ('SP', 'RP', 'CL')",
            nativeQuery = true)
    Page<KboPitchStatInterface> getStatsByPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query(value = "SELECT " +
            " p.seq as id, null as backNo, p.name as name, " +
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
            "WHERE g.season BETWEEN YEAR(:startDate) AND YEAR(:endDate) " +
            "AND p.seq = :id " +
            "AND p.position IN ('SP', 'RP', 'CL') " +
            "GROUP BY p.seq", nativeQuery = true)
    Optional<KboPitchStatInterface> getStatsByPeriodAndId(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("id") Long id);
}
