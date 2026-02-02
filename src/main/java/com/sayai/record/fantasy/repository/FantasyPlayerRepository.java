package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.FantasyPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FantasyPlayerRepository extends JpaRepository<FantasyPlayer, Long> {

    @Query("SELECT p FROM FantasyPlayer p WHERE " +
            "(:team IS NULL OR p.team = :team) AND " +
            "(:position IS NULL OR (:position = 'C' AND p.position = 'C') OR (:position <> 'C' AND p.position LIKE CONCAT('%', :position, '%'))) AND " +
            "(:search IS NULL OR p.name LIKE CONCAT('%', :search, '%'))")
    List<FantasyPlayer> findPlayers(@Param("team") String team,
                                    @Param("position") String position,
                                    @Param("search") String search);
}
