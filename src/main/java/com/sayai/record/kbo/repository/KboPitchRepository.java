package com.sayai.record.kbo.repository;

import com.sayai.record.kbo.model.KboPitch;
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
            " p.PLAYER_ID as id, p.BACK_NO as backNo, p.NAME as name, " +
            " SUM(CASE WHEN pi.result = '승' THEN 1 ELSE 0 END) as wins, " +
            " SUM(CASE WHEN pi.result = '패' THEN 1 ELSE 0 END) as loses, " +
            " SUM(CASE WHEN pi.result = '세' THEN 1 ELSE 0 END) as saves, " +
            " IFNULL(SUM(pi.inning), 0) as inning, " +
            " IFNULL(SUM(pi.batter), 0) as batter, " +
            " IFNULL(SUM(pi.bb), 0) as baseOnBall, " +
            " IFNULL(SUM(pi.hbp), 0) as hitByBall, " +
            " IFNULL(SUM(pi.hit), 0) as pHit, " +
            " IFNULL(SUM(pi.er), 0) as selfLossScore " +
            "FROM kbo_pitch pi " +
            "JOIN player p ON pi.PLAYER_ID = p.PLAYER_ID " +
            "JOIN kbo_game g ON pi.GAME_IDX = g.GAME_IDX " +
            "WHERE g.game_date BETWEEN :startDate AND :endDate " +
            "AND p.sleep_yn = 'N' " +
            "GROUP BY p.PLAYER_ID " +
            "ORDER BY inning DESC", nativeQuery = true)
    List<KboPitchStatInterface> getStatsByPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT " +
            " p.PLAYER_ID as id, p.BACK_NO as backNo, p.NAME as name, " +
            " SUM(CASE WHEN pi.result = '승' THEN 1 ELSE 0 END) as wins, " +
            " SUM(CASE WHEN pi.result = '패' THEN 1 ELSE 0 END) as loses, " +
            " SUM(CASE WHEN pi.result = '세' THEN 1 ELSE 0 END) as saves, " +
            " IFNULL(SUM(pi.inning), 0) as inning, " +
            " IFNULL(SUM(pi.batter), 0) as batter, " +
            " IFNULL(SUM(pi.bb), 0) as baseOnBall, " +
            " IFNULL(SUM(pi.hbp), 0) as hitByBall, " +
            " IFNULL(SUM(pi.hit), 0) as pHit, " +
            " IFNULL(SUM(pi.er), 0) as selfLossScore " +
            "FROM kbo_pitch pi " +
            "JOIN player p ON pi.PLAYER_ID = p.PLAYER_ID " +
            "JOIN kbo_game g ON pi.GAME_IDX = g.GAME_IDX " +
            "WHERE g.game_date BETWEEN :startDate AND :endDate " +
            "AND p.PLAYER_ID = :id " +
            "AND p.sleep_yn = 'N' " +
            "GROUP BY p.PLAYER_ID", nativeQuery = true)
    Optional<KboPitchStatInterface> getStatsByPeriodAndId(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("id") Long id);
}
