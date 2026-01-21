package com.sayai.record.repository;


import com.sayai.record.dto.HitterStatDto;
import com.sayai.record.model.HitterBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HitterBoardRepository extends JpaRepository<HitterBoard, Long> {
    @Query("""
    SELECT new com.sayai.record.dto.HitterStatDto(
        hb.player.id,
        SUM(hb.rbi),
        SUM(hb.runs),
        SUM(hb.sb)
    )
    FROM HitterBoard hb
    WHERE hb.game.id IN (
        SELECT g.id
        FROM Game g
        WHERE g.gameDate BETWEEN :startDate AND :endDate
    )
    GROUP BY hb.player
""")
    List<HitterStatDto> getPlayerByPeriod(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    @Query("""
    SELECT new com.sayai.record.dto.HitterStatDto(
        hb.player.id,
        SUM(hb.rbi),
        SUM(hb.runs),
        SUM(hb.sb)
    )
    FROM HitterBoard hb
    WHERE hb.game.id IN (
        SELECT g.id
        FROM Game g
        WHERE g.gameDate BETWEEN :startDate AND :endDate
    )
    and hb.player.id = :id
    GROUP BY hb.player
""")
    Optional<HitterStatDto> getPlayerByPeriodAndId(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("id") Long id);
}
