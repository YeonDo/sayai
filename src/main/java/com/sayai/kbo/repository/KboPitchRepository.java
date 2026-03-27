package com.sayai.kbo.repository;

import com.sayai.kbo.model.KboPitch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface KboPitchRepository extends JpaRepository<KboPitch, Long> {

    @Query(value = "SELECT " +
            " p.seq as id, null as backNo, p.name as name, " +
            " IFNULL(SUM(pi.inning), 0) as inning, " +
            " IFNULL(SUM(pi.batter), 0) as batter, " +
            " IFNULL(SUM(pi.bb), 0) as baseOnBall, " +
            " IFNULL(SUM(pi.hbp), 0) as hitByBall, " +
            " IFNULL(SUM(pi.hit), 0) as pHit, " +
            " IFNULL(SUM(pi.er), 0) as selfLossScore " +
            "FROM kbo_pitch pi " +
            "JOIN ft_players p ON pi.PLAYER_ID = p.seq " +
            "JOIN kbo_game g ON pi.game_idx = g.game_idx " +
            "WHERE g.season BETWEEN YEAR(:startDate) AND YEAR(:endDate) " +
            "GROUP BY p.seq " +
            "ORDER BY inning DESC", nativeQuery = true)
    List<KboPitchStatInterface> getStatsByPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT " +
            " p.seq as id, null as backNo, p.name as name, " +
            " IFNULL(SUM(pi.inning), 0) as inning, " +
            " IFNULL(SUM(pi.batter), 0) as batter, " +
            " IFNULL(SUM(pi.bb), 0) as baseOnBall, " +
            " IFNULL(SUM(pi.hbp), 0) as hitByBall, " +
            " IFNULL(SUM(pi.hit), 0) as pHit, " +
            " IFNULL(SUM(pi.er), 0) as selfLossScore " +
            "FROM kbo_pitch pi " +
            "JOIN ft_players p ON pi.PLAYER_ID = p.seq " +
            "JOIN kbo_game g ON pi.game_idx = g.game_idx " +
            "WHERE g.season BETWEEN YEAR(:startDate) AND YEAR(:endDate) " +
            "AND p.seq = :id " +
            "GROUP BY p.seq", nativeQuery = true)
    Optional<KboPitchStatInterface> getStatsByPeriodAndId(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("id") Long id);
}
