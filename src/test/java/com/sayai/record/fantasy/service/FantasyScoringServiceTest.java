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
}
