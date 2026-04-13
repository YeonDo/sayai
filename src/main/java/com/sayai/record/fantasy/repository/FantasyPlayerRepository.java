package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.FantasyPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface FantasyPlayerRepository extends JpaRepository<FantasyPlayer, Long> {

    @Query("SELECT p FROM FantasyPlayer p WHERE p.isActive = 0 AND " +
            "(:team IS NULL OR p.team = :team) AND " +
            "(:position IS NULL OR " +
            "  (:position = 'C' AND p.position = 'C') OR " +
            "  (:position = 'IF' AND (p.position LIKE '%1B%' OR p.position LIKE '%2B%' OR p.position LIKE '%3B%' OR p.position LIKE '%SS%')) OR " +
            "  (:position = 'OF' AND (p.position LIKE '%LF%' OR p.position LIKE '%CF%' OR p.position LIKE '%RF%')) OR " +
            "  (:position NOT IN ('C', 'IF', 'OF') AND p.position LIKE CONCAT('%', :position, '%'))) AND " +
            "(:search IS NULL OR p.name LIKE CONCAT('%', :search, '%')) AND " +
            "(:foreignerType IS NULL OR p.foreignerType = :foreignerType)")
    List<FantasyPlayer> findPlayers(@Param("team") String team,
                                    @Param("position") String position,
                                    @Param("search") String search,
                                    @Param("foreignerType") FantasyPlayer.ForeignerType foreignerType);

    @Query("SELECT p FROM FantasyPlayer p WHERE p.isActive = 0 AND p.seq NOT IN :seqs")
    List<FantasyPlayer> findBySeqNotIn(@Param("seqs") Collection<Long> seqs);

    @Query("SELECT p FROM FantasyPlayer p WHERE p.isActive = 0")
    List<FantasyPlayer> findAllActivePlayers();
}
