package com.sayai.kbo.repository;

import com.sayai.kbo.model.KboPitcherStats;
import com.sayai.kbo.model.KboPitcherStatsId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface KboPitcherStatsRepository extends JpaRepository<KboPitcherStats, KboPitcherStatsId> {

    Optional<KboPitcherStats> findByPlayerIdAndSeason(Long playerId, Integer season);

    List<KboPitcherStats> findByPlayerIdInAndSeason(Collection<Long> playerIds, Integer season);

    @Query(value = "SELECT s.player_id as id, p.name as name, p.team as team, " +
            "s.outs as outs, s.er as er, s.era as era, s.win as win, " +
            "s.so as so, s.save as save, s.bb as bb, s.phit as phit, s.whip as whip " +
            "FROM kbo_pitcher_stats s " +
            "JOIN ft_players p ON s.player_id = p.seq " +
            "WHERE s.season = :season " +
            "AND (:minOuts IS NULL OR s.outs >= :minOuts)",
            countQuery = "SELECT COUNT(*) FROM kbo_pitcher_stats s " +
            "WHERE s.season = :season AND (:minOuts IS NULL OR s.outs >= :minOuts)",
            nativeQuery = true)
    Page<KboPitcherSeasonStatsProjection> findBySeasonWithPlayerInfo(
            @Param("season") Integer season,
            @Param("minOuts") Integer minOuts,
            Pageable pageable);

    @Query(value = "SELECT s.player_id as id, p.name as name, p.team as team, " +
            "s.outs as outs, s.er as er, s.era as era, s.win as win, " +
            "s.so as so, s.save as save, s.bb as bb, s.phit as phit, s.whip as whip " +
            "FROM kbo_pitcher_stats s " +
            "JOIN ft_players p ON s.player_id = p.seq " +
            "WHERE s.season = :season " +
            "AND (:minOuts IS NULL OR s.outs >= :minOuts) " +
            "AND p.position REGEXP :positionPattern",
            countQuery = "SELECT COUNT(*) FROM kbo_pitcher_stats s " +
            "JOIN ft_players p ON s.player_id = p.seq " +
            "WHERE s.season = :season AND (:minOuts IS NULL OR s.outs >= :minOuts) AND p.position REGEXP :positionPattern",
            nativeQuery = true)
    Page<KboPitcherSeasonStatsProjection> findBySeasonWithPlayerInfoAndPositions(
            @Param("season") Integer season,
            @Param("minOuts") Integer minOuts,
            @Param("positionPattern") String positionPattern,
            Pageable pageable);
}
