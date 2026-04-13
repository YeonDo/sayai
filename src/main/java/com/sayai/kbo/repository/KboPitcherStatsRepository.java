package com.sayai.kbo.repository;

import com.sayai.kbo.model.KboPitcherStats;
import com.sayai.kbo.model.KboPitcherStatsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KboPitcherStatsRepository extends JpaRepository<KboPitcherStats, KboPitcherStatsId> {

    Optional<KboPitcherStats> findByPlayerIdAndSeason(Long playerId, Integer season);
}
