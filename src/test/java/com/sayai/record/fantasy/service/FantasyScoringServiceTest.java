package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.FantasyScoreDto;
import com.sayai.record.fantasy.entity.FantasyRotisserieScore;
import com.sayai.record.fantasy.repository.FantasyRotisserieScoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
public class FantasyScoringServiceTest {

    @Autowired
    private FantasyScoringService scoringService;

    @Autowired
    private FantasyRotisserieScoreRepository scoreRepository;

    @Test
    public void testSaveAndCalculateScores() {
        Long gameSeq = 999L;
        Integer round = 1;
        int playerCount = 10;

        // 1. Pre-populate DB with existing scores
        List<FantasyRotisserieScore> initialScores = new ArrayList<>();
        for (long i = 1; i <= playerCount; i++) {
            initialScores.add(FantasyRotisserieScore.builder()
                    .fantasyGameSeq(gameSeq)
                    .playerId(i)
                    .round(round)
                    .avg(0.250)
                    .hr(0)
                    .rbi(0)
                    .build());
        }
        scoreRepository.saveAll(initialScores);
        scoreRepository.flush();

        // 2. Prepare input DTOs for update
        List<FantasyScoreDto> inputScores = new ArrayList<>();
        for (long i = 1; i <= playerCount; i++) {
            inputScores.add(FantasyScoreDto.builder()
                    .fantasyGameSeq(gameSeq)
                    .playerId(i)
                    .round(round)
                    .avg(0.300) // Changed value
                    .hr(1)
                    .rbi(5)
                    .soBatter(2)
                    .sb(1)
                    .wins(0)
                    .era(3.5)
                    .soPitcher(5)
                    .whip(1.1)
                    .saves(0)
                    .build());
        }

        // 3. Execute
        scoringService.saveAndCalculateScores(gameSeq, round, inputScores);

        // 4. Verify Update
        List<FantasyRotisserieScore> updatedScores = scoreRepository.findByFantasyGameSeqAndRound(gameSeq, round);
        assertEquals(playerCount, updatedScores.size());
        assertEquals(0.300, updatedScores.get(0).getAvg(), 0.001);
    }

    @Test
    public void testNullScoreHandling() {
        Long gameSeq = 888L;
        Integer round = 1;
        int playerCount = 3;

        // 1. Create 3 players
        List<FantasyRotisserieScore> initialScores = new ArrayList<>();
        for (long i = 1; i <= playerCount; i++) {
            initialScores.add(FantasyRotisserieScore.builder()
                    .fantasyGameSeq(gameSeq)
                    .playerId(i)
                    .round(round)
                    .build());
        }
        scoreRepository.saveAll(initialScores);
        scoreRepository.flush();

        // 2. Input DTOs
        // Player 1: Best (AVG 0.350)
        // Player 2: Middle (AVG 0.250)
        // Player 3: Null (AVG null)
        List<FantasyScoreDto> inputScores = new ArrayList<>();

        inputScores.add(FantasyScoreDto.builder().fantasyGameSeq(gameSeq).playerId(1L).round(round)
                .avg(0.350).rbi(10).hr(5).soBatter(10).sb(5)
                .wins(1).era(2.00).soPitcher(10).whip(1.00).saves(1)
                .build());

        inputScores.add(FantasyScoreDto.builder().fantasyGameSeq(gameSeq).playerId(2L).round(round)
                .avg(0.250).rbi(5).hr(2).soBatter(20).sb(2) // soBatter higher (worse)
                .wins(0).era(4.00).soPitcher(5).whip(1.50).saves(0)
                .build());

        inputScores.add(FantasyScoreDto.builder().fantasyGameSeq(gameSeq).playerId(3L).round(round)
                .avg(null).rbi(null).hr(null).soBatter(null).sb(null)
                .wins(null).era(null).soPitcher(null).whip(null).saves(null)
                .build());

        // 3. Execute
        scoringService.saveAndCalculateScores(gameSeq, round, inputScores);

        // 4. Verify
        List<FantasyRotisserieScore> scores = scoreRepository.findByFantasyGameSeqAndRound(gameSeq, round);

        // Find by playerId
        FantasyRotisserieScore p1 = scores.stream().filter(s -> s.getPlayerId() == 1L).findFirst().orElseThrow();
        FantasyRotisserieScore p2 = scores.stream().filter(s -> s.getPlayerId() == 2L).findFirst().orElseThrow();
        FantasyRotisserieScore p3 = scores.stream().filter(s -> s.getPlayerId() == 3L).findFirst().orElseThrow();

        // Check AVG
        // P1: Rank 1, Points (3-1+1)*10 = 30
        assertEquals(1, p1.getRankAvg());
        assertEquals(30.0, p1.getPointsAvg(), 0.01);

        // P2: Rank 2, Points (3-2+1)*10 = 20
        assertEquals(2, p2.getRankAvg());
        assertEquals(20.0, p2.getPointsAvg(), 0.01);

        // P3: Rank 3, Points 0 (As per requirement)
        assertEquals(3, p3.getRankAvg(), "Null score should get last rank");
        assertEquals(0.0, p3.getPointsAvg(), 0.01, "Null score should get 0 points");
    }
}
