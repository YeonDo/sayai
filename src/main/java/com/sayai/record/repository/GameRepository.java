package com.sayai.record.repository;

import com.sayai.record.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findFirstByOrderByGameDateDesc();

    List<Game> findAllByGameDateBetweenOrderByGameDateAscGameTimeAsc(LocalDate startDate, LocalDate endDate);

    List<Game> findByOpponentContainingOrderByGameDateAsc(String opponent);
    @Query(value = "select * from game where match (opponent) against (?1)",nativeQuery = true)
    List<Game> findAllByOpponentMatchAgainst(String opponent);
}
