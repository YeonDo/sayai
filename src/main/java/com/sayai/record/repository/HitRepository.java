package com.sayai.record.repository;

import com.sayai.record.dto.PlayerDto;
import com.sayai.record.dto.PlayerInterface;
import com.sayai.record.model.Hit;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HitRepository extends JpaRepository<Hit, Long> {
    @Query(value = "SELECT " +
            " A.PLAYER_ID as id , BACK_NO as backNo" +
            " , NAME " +
            " , GAME_CNT as totalGames" +
            " , BATER_CNT as playerAppearance" +
            " , (BATER_CNT / GAME_CNT) as avgPa " +
            " , (OUT_CNT + MIS_CNT + HIT_CNT) as atBat " +
            " , ROUND( HIT_CNT / (OUT_CNT + MIS_CNT + HIT_CNT), 3) as battingAvg " +
            " , ROUND( (HIT_CNT + OB_CNT) / BATER_CNT, 3)  as onBasePer" +
            " , ROUND( ONBASE / (OUT_CNT + MIS_CNT + HIT_CNT), 3) as slugPer " +
            " , (HIT_1 + HIT_2 + HIT_3 + HIT_4) as totalHits" +
            " , HIT_1 as singles" +
            " , HIT_2 as doubles" +
            " , HIT_3 as triples" +
            " , HIT_4 as homeruns " +
            "FROM  " +
            "( " +
            " SELECT " +
            " PLAYER_ID " +
            " , SUM(CASE WHEN CD = 'H' THEN 1 END) AS HIT_CNT " +
            " , SUM(CASE WHEN CD = 'O' THEN 1 END) AS OUT_CNT " +
            " , SUM(CASE WHEN CD = 'S' THEN 1 END) AS SAC_CNT " +
            " , SUM(CASE WHEN CD = 'M' THEN 1 END) AS MIS_CNT " +
            " , SUM(CASE WHEN CD = 'B' THEN 1 END) AS OB_CNT " +
            " , SUM(CASE WHEN ON_BASE = 1 THEN 1 ELSE 0 END) AS HIT_1 " +
            " , SUM(CASE WHEN ON_BASE = 2 THEN 1 ELSE 0 END) AS HIT_2 " +
            " , SUM(CASE WHEN ON_BASE = 3 THEN 1 ELSE 0 END) AS HIT_3 " +
            " , SUM(CASE WHEN ON_BASE = 4 THEN 1 ELSE 0 END) AS HIT_4 " +
            " , SUM(ON_BASE) AS ONBASE " +
            " , COUNT(DISTINCT GAME_IDX) AS GAME_CNT " +
            " , COUNT(*) as BATER_CNT " +
            " FROM  " +
            " ( " +
            " SELECT " +
            " A.PLAYER_ID " +
            " , GAME_IDX " +
            " , CASE WHEN R_HIT_CD IN ('1', '2', '3', '4') THEN 'H' " +
            "       WHEN R_HIT_CD IN ('0', '00', '000') THEN 'O' " +
            "       WHEN R_HIT_CD IN ('8') THEN 'S' " +
            "       WHEN R_HIT_CD IN ('5') THEN 'M' " +
            "       WHEN R_HIT_CD IN ('7') THEN 'B' end CD" +
            " , CASE WHEN R_HIT_CD IN ('1','2','3','4') THEN R_HIT_CD ELSE 0 END ON_BASE " +
            " FROM HIT A, HIT_CD B " +
            " WHERE A.HIT_CD = B.HIT_CD " +
            " AND GAME_IDX IN (SELECT GAME_IDX FROM GAME g WHERE GAME_DATE between :startDate AND :endDate) " +
            " ) A " +
            " GROUP BY PLAYER_ID " +
            ") A, PLAYER B " +
            "WHERE A.PLAYER_ID = B.PLAYER_ID " +
            "ORDER BY 7 DESC", nativeQuery = true)
    List<PlayerInterface> getPlayerByPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    @Query(value = "SELECT " +
            " A.PLAYER_ID as id , BACK_NO as backNo" +
            " , NAME " +
            " , GAME_CNT as totalGames" +
            " , BATER_CNT as playerAppearance" +
            " , (BATER_CNT / GAME_CNT) as avgPa " +
            " , (OUT_CNT + MIS_CNT + HIT_CNT) as atBat " +
            " , ROUND( HIT_CNT / (OUT_CNT + MIS_CNT + HIT_CNT), 3) as battingAvg " +
            " , ROUND( (HIT_CNT + OB_CNT) / BATER_CNT, 3)  as onBasePer" +
            " , ROUND( ONBASE / (OUT_CNT + MIS_CNT + HIT_CNT), 3) as slugPer " +
            " , (HIT_1 + HIT_2 + HIT_3 + HIT_4) as totalHits" +
            " , HIT_1 as singles" +
            " , HIT_2 as doubles" +
            " , HIT_3 as triples" +
            " , HIT_4 as homeruns " +
            "FROM  " +
            "( " +
            " SELECT " +
            " PLAYER_ID " +
            " , SUM(CASE WHEN CD = 'H' THEN 1 END) AS HIT_CNT " +
            " , SUM(CASE WHEN CD = 'O' THEN 1 END) AS OUT_CNT " +
            " , SUM(CASE WHEN CD = 'S' THEN 1 END) AS SAC_CNT " +
            " , SUM(CASE WHEN CD = 'M' THEN 1 END) AS MIS_CNT " +
            " , SUM(CASE WHEN CD = 'B' THEN 1 END) AS OB_CNT " +
            " , SUM(CASE WHEN ON_BASE = 1 THEN 1 ELSE 0 END) AS HIT_1 " +
            " , SUM(CASE WHEN ON_BASE = 2 THEN 1 ELSE 0 END) AS HIT_2 " +
            " , SUM(CASE WHEN ON_BASE = 3 THEN 1 ELSE 0 END) AS HIT_3 " +
            " , SUM(CASE WHEN ON_BASE = 4 THEN 1 ELSE 0 END) AS HIT_4 " +
            " , SUM(ON_BASE) AS ONBASE " +
            " , COUNT(DISTINCT GAME_IDX) AS GAME_CNT " +
            " , COUNT(*) as BATER_CNT " +
            " FROM  " +
            " ( " +
            " SELECT " +
            " A.PLAYER_ID " +
            " , GAME_IDX " +
            " , CASE WHEN R_HIT_CD IN ('1', '2', '3', '4') THEN 'H' " +
            "       WHEN R_HIT_CD IN ('0', '00', '000') THEN 'O' " +
            "       WHEN R_HIT_CD IN ('8') THEN 'S' " +
            "       WHEN R_HIT_CD IN ('5') THEN 'M' " +
            "       WHEN R_HIT_CD IN ('7') THEN 'B' end CD" +
            " , CASE WHEN R_HIT_CD IN ('1','2','3','4') THEN R_HIT_CD ELSE 0 END ON_BASE " +
            " FROM HIT A, HIT_CD B " +
            " WHERE A.HIT_CD = B.HIT_CD " +
            " AND GAME_IDX IN (SELECT GAME_IDX FROM GAME g WHERE GAME_DATE between :startDate AND :endDate) " +
            " ) A " +
            " GROUP BY PLAYER_ID " +
            ") A, PLAYER B " +
            "WHERE A.PLAYER_ID = B.PLAYER_ID and A.PLAYER_ID = :id " +
            "ORDER BY 7 DESC", nativeQuery = true)
    Optional<PlayerInterface> getPlayerByPeriodAndId(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("id") Long id);
}
