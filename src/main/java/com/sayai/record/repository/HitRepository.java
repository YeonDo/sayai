package com.sayai.record.repository;

import com.sayai.record.dto.PlayerDto;
import com.sayai.record.dto.PlayerInterface;
import com.sayai.record.model.Hit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
            " , BB as baseOnBall" +
            " , HBP as hitByPitch " +
            " , SO as strikeOut " +
            " , IBB as ibb " +
            " , DP as dp " +
            " , SAC as sacrifice " +
            " , SAP_F as sacFly " +
            "FROM  " +
            "( " +
            " SELECT " +
            " PLAYER_ID " +
            " , IFNULL(SUM(CASE WHEN CD = 'H' THEN 1 END),0) AS HIT_CNT " +
            " , IFNULL(SUM(CASE WHEN CD = 'O' THEN 1 END),0) AS OUT_CNT " +
            " , IFNULL(SUM(CASE WHEN CD = 'S' THEN 1 END),0) AS SAC_CNT " +
            " , IFNULL(SUM(CASE WHEN CD = 'M' THEN 1 END),0) AS MIS_CNT " +
            " , IFNULL(SUM(CASE WHEN CD = 'B' THEN 1 END),0) AS OB_CNT " +
            " , IFNULL(SUM(CASE WHEN ON_BASE = 1 THEN 1 ELSE 0 END),0) AS HIT_1 " +
            " , IFNULL(SUM(CASE WHEN ON_BASE = 2 THEN 1 ELSE 0 END),0) AS HIT_2 " +
            " , IFNULL(SUM(CASE WHEN ON_BASE = 3 THEN 1 ELSE 0 END),0) AS HIT_3 " +
            " , IFNULL(SUM(CASE WHEN ON_BASE = 4 THEN 1 ELSE 0 END),0) AS HIT_4 " +
            " , IFNULL(SUM(ON_BASE),0) AS ONBASE " +
            " , IFNULL(SUM(SO), 0) AS SO " +
            " , IFNULL(SUM(BB), 0) AS BB " +
            " , IFNULL(SUM(HBP), 0) AS HBP " +
            " , IFNULL(SUM(IBB), 0) AS IBB " +
            " , IFNULL(SUM(DP), 0) AS DP " +
            " , IFNULL(SUM(SAC), 0) AS SAC " +
            " , IFNULL(SUM(SAP_F), 0) AS SAP_F " +
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
            " , CASE WHEN A.HIT_CD IN ('10', '20', '21' ,'30') THEN 1 END AS SO " +
            " , CASE WHEN A.HIT_CD = '41' THEN 1 END AS BB" +
            " , CASE WHEN A.HIT_CD = '22' THEN 1 END AS HBP " +
            " , CASE WHEN A.HIT_CD = '31' THEN 1 END AS IBB " +
            " , CASE WHEN B.HIT_CD = '00' OR B.P_HIT_CD = '00' THEN 1 END AS DP " +
            " , CASE WHEN R_HIT_CD = '8' THEN 1 END AS SAC " +
            " , CASE WHEN P_HIT_CD IN ('8', '38', '38F', '87') THEN 1 END AS SAP_F "+
            " FROM hit A, HIT_CD B " +
            " WHERE A.HIT_CD = B.HIT_CD " +
            " AND GAME_IDX IN (SELECT GAME_IDX FROM game WHERE GAME_DATE between :startDate AND :endDate) " +
            " ) A " +
            " GROUP BY PLAYER_ID " +
            ") A, player B " +
            "WHERE A.PLAYER_ID = B.PLAYER_ID and B.sleep_yn = 'N' " +
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
            " , BB as baseOnBall" +
            " , HBP as hitByPitch " +
            " , SO as strikeOut " +
            " , IBB as ibb " +
            " , DP as dp " +
            " , SAC as sacrifice " +
            " , SAP_F as sacFly " +
            "FROM  " +
            "( " +
            " SELECT " +
            " PLAYER_ID " +
            " , IFNULL(SUM(CASE WHEN CD = 'H' THEN 1 END),0) AS HIT_CNT " +
            " , IFNULL(SUM(CASE WHEN CD = 'O' THEN 1 END),0) AS OUT_CNT " +
            " , IFNULL(SUM(CASE WHEN CD = 'S' THEN 1 END),0) AS SAC_CNT " +
            " , IFNULL(SUM(CASE WHEN CD = 'M' THEN 1 END),0) AS MIS_CNT " +
            " , IFNULL(SUM(CASE WHEN CD = 'B' THEN 1 END),0) AS OB_CNT " +
            " , IFNULL(SUM(CASE WHEN ON_BASE = 1 THEN 1 ELSE 0 END),0) AS HIT_1 " +
            " , IFNULL(SUM(CASE WHEN ON_BASE = 2 THEN 1 ELSE 0 END),0) AS HIT_2 " +
            " , IFNULL(SUM(CASE WHEN ON_BASE = 3 THEN 1 ELSE 0 END),0) AS HIT_3 " +
            " , IFNULL(SUM(CASE WHEN ON_BASE = 4 THEN 1 ELSE 0 END),0) AS HIT_4 " +
            " , IFNULL(SUM(ON_BASE),0) AS ONBASE " +
            " , IFNULL(SUM(SO), 0) AS SO " +
            " , IFNULL(SUM(BB), 0) AS BB " +
            " , IFNULL(SUM(HBP), 0) AS HBP " +
            " , IFNULL(SUM(IBB), 0) AS IBB " +
            " , IFNULL(SUM(DP), 0) AS DP " +
            " , IFNULL(SUM(SAC), 0) AS SAC " +
            " , IFNULL(SUM(SAP_F), 0) AS SAP_F " +
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
            " , CASE WHEN A.HIT_CD IN ('10', '20', '21' ,'30') THEN 1 END AS SO" +
            " , CASE WHEN A.HIT_CD = '41' THEN 1 END AS BB" +
            " , CASE WHEN A.HIT_CD = '22' THEN 1 END AS HBP " +
            " , CASE WHEN A.HIT_CD = '31' THEN 1 END AS IBB " +
            " , CASE WHEN B.HIT_CD = '00' OR B.P_HIT_CD = '00' THEN 1 END AS DP " +
            " , CASE WHEN R_HIT_CD = '8' THEN 1 END AS SAC " +
            " , CASE WHEN P_HIT_CD IN ('8', '38', '38F', '87') THEN 1 END AS SAP_F "+
            " FROM hit A, HIT_CD B " +
            " WHERE A.HIT_CD = B.HIT_CD " +
            " AND GAME_IDX IN (SELECT GAME_IDX FROM game WHERE GAME_DATE between :startDate AND :endDate) " +
            " ) A " +
            " GROUP BY PLAYER_ID " +
            ") A, player B " +
            "WHERE A.PLAYER_ID = B.PLAYER_ID and A.PLAYER_ID = :id and B.sleep_yn = 'N' " +
            "ORDER BY 7 DESC", nativeQuery = true)
    Optional<PlayerInterface> getPlayerByPeriodAndId(@Param("startDate") LocalDate startDate, @Param("endDate")LocalDate endDate, @Param("id") Long id);
}
