package com.sayai.kbo.repository;

import com.sayai.kbo.model.KboHitterStats;
import com.sayai.kbo.model.KboHitterStatsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KboHitterStatsRepository extends JpaRepository<KboHitterStats, KboHitterStatsId> {

    Optional<KboHitterStats> findByPlayerIdAndSeason(Long playerId, Integer season);
}
