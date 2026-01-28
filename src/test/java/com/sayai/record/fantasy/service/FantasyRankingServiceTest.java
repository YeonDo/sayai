package com.sayai.record.fantasy.service;

import com.sayai.record.dto.PitcherDto;
import com.sayai.record.dto.PlayerDto;
import com.sayai.record.fantasy.dto.RankingTableDto;
import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
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
        Long fantasyPlayerId = 50L;
        Long realPlayerId = 10L;

        // Mock Game
        FantasyGame game = mock(FantasyGame.class);
        when(game.getScoringType()).thenReturn(FantasyGame.ScoringType.ROTISSERIE);
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        // Mock Participant
        FantasyParticipant participant = FantasyParticipant.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(participantId)
                .teamName("My Team")
                .build();
        when(participantRepository.findByFantasyGameSeq(gameSeq)).thenReturn(List.of(participant));

        // Mock Picks
        DraftPick pick = DraftPick.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(participantId)
                .fantasyPlayerSeq(fantasyPlayerId)
                .build();
        when(draftPickRepository.findByFantasyGameSeq(gameSeq)).thenReturn(List.of(pick));

        // Mock Fantasy Player
        FantasyPlayer fantasyPlayer = FantasyPlayer.builder()
                .seq(fantasyPlayerId)
                .name("Test Player")
                .build();
        when(fantasyPlayerRepository.findAllById(any())).thenReturn(List.of(fantasyPlayer));

        // Mock Real Player
        Player realPlayer = Player.builder()
                .id(realPlayerId)
                .name("Test Player")
                .build();
        when(playerRepository.findAll()).thenReturn(List.of(realPlayer));

        // Mock Hit Stats
        PlayerDto hStats = PlayerDto.builder()
                .id(realPlayerId)
                .atBat(10L)
                .totalHits(3L)
                .homeruns(1L)
                .rbi(2)
                .sb(0)
                .strikeOut(2L)
                .build();
        when(hitService.findAllByPeriod(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(hStats));

        // Mock Pitch Stats
        PitcherDto pStats = PitcherDto.builder()
                .id(realPlayerId)
                .wins(1L)
                .stOut(5L)
                .saves(0L)
                .selfLossScore(2L)
                .inn(9L) // 3 innings (9 outs)
                .pHit(2L)
                .baseOnBall(1L)
                .build();
        when(pitchRepository.getStatsByPeriod(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(pStats));

        // Execute
        RankingTableDto result = fantasyRankingService.getRanking(gameSeq);

        // Verify
        assertEquals(1, result.getRankings().size());
        var stats = result.getRankings().get(0);

        // Hitter Checks
        assertEquals(0.3, stats.getBattingAvg(), 0.001); // 3/10
        assertEquals(1, stats.getHomeruns());
        assertEquals(2, stats.getRbi());
        assertEquals(2, stats.getBatterStrikeOuts());

        // Pitcher Checks
        assertEquals(1, stats.getWins());
        assertEquals(5, stats.getPitcherStrikeOuts());
        // ERA = (ER * 27) / Outs = (2 * 27) / 9 = 54 / 9 = 6.0
        assertEquals(6.0, stats.getEra(), 0.001);
        // WHIP = (H + BB) * 3 / Outs = (2+1)*3 / 9 = 9/9 = 1.0
        assertEquals(1.0, stats.getWhip(), 0.001);
    }
}
