package com.sayai.kbo.repository;

import com.sayai.kbo.model.KboHitterStats;
import com.sayai.kbo.model.KboHitterStatsId;
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
public interface KboHitterStatsRepository extends JpaRepository<KboHitterStats, KboHitterStatsId> {

    Optional<KboHitterStats> findByPlayerIdAndSeason(Long playerId, Integer season);

    List<KboHitterStats> findByPlayerIdInAndSeason(Collection<Long> playerIds, Integer season);

    @Query(value = "SELECT s.player_id as id, p.name as name, p.team as team, " +
            "s.pa as pa, s.ab as ab, s.hit as hit, s.avg as avg, " +
            "s.hr as hr, s.rbi as rbi, s.so as so, s.sb as sb " +
            "FROM kbo_hitter_stats s " +
            "JOIN ft_players p ON s.player_id = p.seq " +
            "WHERE s.season = :season " +
            "AND (:minPa IS NULL OR s.pa >= :minPa)",
            countQuery = "SELECT COUNT(*) FROM kbo_hitter_stats s " +
            "WHERE s.season = :season AND (:minPa IS NULL OR s.pa >= :minPa)",
            nativeQuery = true)
    Page<KboHitterSeasonStatsProjection> findBySeasonWithPlayerInfo(
            @Param("season") Integer season,
            @Param("minPa") Integer minPa,
            Pageable pageable);

    @Query(value = "SELECT s.player_id as id, p.name as name, p.team as team, " +
            "s.pa as pa, s.ab as ab, s.hit as hit, s.avg as avg, " +
            "s.hr as hr, s.rbi as rbi, s.so as so, s.sb as sb " +
            "FROM kbo_hitter_stats s " +
            "JOIN ft_players p ON s.player_id = p.seq " +
            "WHERE s.season = :season " +
            "AND (:minPa IS NULL OR s.pa >= :minPa) " +
            "AND p.position REGEXP :positionPattern",
            countQuery = "SELECT COUNT(*) FROM kbo_hitter_stats s " +
            "JOIN ft_players p ON s.player_id = p.seq " +
            "WHERE s.season = :season AND (:minPa IS NULL OR s.pa >= :minPa) AND p.position REGEXP :positionPattern",
            nativeQuery = true)
    Page<KboHitterSeasonStatsProjection> findBySeasonWithPlayerInfoAndPositions(
            @Param("season") Integer season,
            @Param("minPa") Integer minPa,
            @Param("positionPattern") String positionPattern,
            Pageable pageable);
}
