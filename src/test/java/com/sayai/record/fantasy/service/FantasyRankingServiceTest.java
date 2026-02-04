package com.sayai.record.fantasy.service;

import com.sayai.record.dto.PitcherDto;
import com.sayai.record.dto.PlayerDto;
import com.sayai.record.fantasy.dto.RankingTableDto;
import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.entity.FantasyRotisserieScore;
import com.sayai.record.fantasy.repository.*;
import com.sayai.record.model.Player;
import com.sayai.record.repository.PitchRepository;
import com.sayai.record.repository.PlayerRepository;
import com.sayai.record.service.HitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FantasyRankingServiceTest {

    @Mock private FantasyGameRepository fantasyGameRepository;
    @Mock private FantasyParticipantRepository participantRepository;
    @Mock private DraftPickRepository draftPickRepository;
    @Mock private FantasyPlayerRepository fantasyPlayerRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private HitService hitService;
    @Mock private PitchRepository pitchRepository;
    @Mock private FantasyRotisserieScoreRepository rotisserieScoreRepository;

    @InjectMocks
    private FantasyRankingService fantasyRankingService;

    @Test
    void getRanking_ReturnEmptyForPointsGame() {
        Long gameSeq = 1L;
        FantasyGame game = mock(FantasyGame.class);
        when(game.getScoringType()).thenReturn(FantasyGame.ScoringType.POINTS);
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        RankingTableDto result = fantasyRankingService.getRanking(gameSeq);

        assertEquals("POINTS", result.getScoringType());
        assertEquals(0, result.getRankings().size());
    }

    @Test
    void getRanking_AggregatesStatsCorrectly() {
        Long gameSeq = 1L;
        Long participantId = 100L;

        // Mock Game
        FantasyGame game = mock(FantasyGame.class);
        when(game.getSeq()).thenReturn(gameSeq);
        when(game.getScoringType()).thenReturn(FantasyGame.ScoringType.ROTISSERIE);
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        // Mock Participant
        FantasyParticipant participant = FantasyParticipant.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(participantId)
                .teamName("My Team")
                .build();
        when(participantRepository.findByFantasyGameSeq(gameSeq)).thenReturn(List.of(participant));

        // Mock Scores (2 rounds)
        FantasyRotisserieScore s1 = FantasyRotisserieScore.builder()
                .fantasyGameSeq(gameSeq).playerId(participantId).round(1)
                .avg(0.300).hr(1).rbi(2).soBatter(2).sb(0)
                .wins(1).soPitcher(5).era(6.0).whip(1.0).saves(0)
                .totalPoints(50.0)
                .build();

        FantasyRotisserieScore s2 = FantasyRotisserieScore.builder()
                .fantasyGameSeq(gameSeq).playerId(participantId).round(2)
                .avg(0.400).hr(2).rbi(3).soBatter(1).sb(1)
                .wins(0).soPitcher(2).era(2.0).whip(0.5).saves(1)
                .totalPoints(60.0)
                .build();

        when(rotisserieScoreRepository.findByFantasyGameSeq(gameSeq)).thenReturn(List.of(s1, s2));

        // Execute
        RankingTableDto result = fantasyRankingService.getRanking(gameSeq);

        // Verify
        assertEquals(1, result.getRankings().size());
        var stats = result.getRankings().get(0);

        // Total Points
        assertEquals(110.0, stats.getTotalPoints(), 0.001);

        // Summed Stats
        assertEquals(3, stats.getHomeruns()); // 1+2
        assertEquals(5, stats.getRbi()); // 2+3
        assertEquals(1, stats.getStolenBases()); // 0+1
        assertEquals(3, stats.getBatterStrikeOuts()); // 2+1
        assertEquals(1, stats.getWins()); // 1+0
        assertEquals(7, stats.getPitcherStrikeOuts()); // 5+2
        assertEquals(1, stats.getSaves()); // 0+1

        // Averaged Stats
        assertEquals(0.350, stats.getBattingAvg(), 0.001); // (0.3+0.4)/2
        assertEquals(4.0, stats.getEra(), 0.001); // (6.0+2.0)/2
        assertEquals(0.75, stats.getWhip(), 0.001); // (1.0+0.5)/2
    }
}
