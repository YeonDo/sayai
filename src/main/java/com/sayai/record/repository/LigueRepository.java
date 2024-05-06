package com.sayai.record.repository;

import com.sayai.record.model.Ligue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LigueRepository extends JpaRepository<Ligue, Long> {

    Optional<Ligue> findByName(String name);
    @Query("select l from Ligue l where l.season = :season and (l.name = :name or l.nameSec = :name)")
    Optional<Ligue> findBySeasonAndNameOrNameSec(@Param("season") Long season, @Param("name") String name);
}
