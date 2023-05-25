package com.sayai.record.repository;

import com.sayai.record.dto.PitcherDto;
import com.sayai.record.model.Pitch;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PitchRepository extends JpaRepository<Pitch, Long> {
    @Query("SELECT new com.sayai.record.dto.PitcherDto(" +
            "pl.id, pl.backNo, pl.name, " +
            "SUM(CASE WHEN p.result = '승' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN p.result = '패' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN p.result = '세' THEN 1 ELSE 0 END), " +
            "SUM(p.inning),SUM(p.batter) , SUM(p.hitter) , "+
            "SUM(p.pHit) , SUM(p.pHomerun) ,"+
            "SUM(p.sacrifice) , SUM(p.sacFly) , "+
            "SUM(p.baseOnBall) , SUM(p.hitByBall) , " +
            "SUM(p.stOut) , SUM(p.fallingBall) , "+
            "SUM(p.balk) , SUM(p.lossScore) , SUM(p.selfLossScore)) " +
            "FROM Pitch p join p.game g join p.player pl " +
            "WHERE g.gameDate BETWEEN :startDate AND :endDate " +
            "GROUP BY p.player")
    List<PitcherDto> getStatsByPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    @Query("SELECT new com.sayai.record.dto.PitcherDto(" +
            "pl.id, pl.backNo, pl.name, " +
            "SUM(CASE WHEN p.result = '승' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN p.result = '패' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN p.result = '세' THEN 1 ELSE 0 END), " +
            "SUM(p.inning),SUM(p.batter) , SUM(p.hitter) , "+
            "SUM(p.pHit) , SUM(p.pHomerun) ,"+
            "SUM(p.sacrifice) , SUM(p.sacFly) , "+
            "SUM(p.baseOnBall) , SUM(p.hitByBall) , " +
            "SUM(p.stOut) , SUM(p.fallingBall) , "+
            "SUM(p.balk) , SUM(p.lossScore) , SUM(p.selfLossScore)) " +
            "FROM Pitch p join p.game g join p.player pl " +
            "WHERE g.gameDate BETWEEN :startDate AND :endDate " +
            "and pl.id = :id " +
            "GROUP BY p.player")
    Optional<PitcherDto> getStats(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("id") Long id);

}
