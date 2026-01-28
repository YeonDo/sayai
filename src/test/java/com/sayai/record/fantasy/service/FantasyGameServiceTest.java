package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.FantasyGameDetailDto;
import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FantasyGameServiceTest {

    @Mock private FantasyGameRepository fantasyGameRepository;
    @Mock private FantasyParticipantRepository participantRepository;
    @Mock private DraftPickRepository draftPickRepository;
    @Mock private FantasyPlayerRepository fantasyPlayerRepository;

    @InjectMocks
    private FantasyGameService fantasyGameService;

    @Test
    void getGameDetails_ReturnsCorrectStructure() {
        Long gameSeq = 1L;
        Long participantId = 100L;
        Long fantasyPlayerSeq = 50L;

        // Mock Game
        FantasyGame game = mock(FantasyGame.class);
        when(game.getSeq()).thenReturn(gameSeq);
        when(game.getTitle()).thenReturn("Test Game");
        when(game.getRuleType()).thenReturn(FantasyGame.RuleType.RULE_1);
        when(game.getScoringType()).thenReturn(FantasyGame.ScoringType.POINTS);
        when(game.getStatus()).thenReturn(FantasyGame.GameStatus.DRAFTING);
        when(game.getGameDuration()).thenReturn("2026-03-01 ~ 2026-10-30");
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        // Mock Participant
        FantasyParticipant participant = FantasyParticipant.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(participantId)
                .teamName("Team A")
                .build();
        when(participantRepository.findByFantasyGameSeq(gameSeq)).thenReturn(List.of(participant));

        // Mock Picks
        DraftPick pick = DraftPick.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(participantId)
                .fantasyPlayerSeq(fantasyPlayerSeq)
                .build();
        when(draftPickRepository.findByFantasyGameSeq(gameSeq)).thenReturn(List.of(pick));

        // Mock Fantasy Player
        FantasyPlayer fantasyPlayer = FantasyPlayer.builder()
                .seq(fantasyPlayerSeq)
                .name("Player 1")
                .position("1B")
                .team("KIA")
                .build();
        when(fantasyPlayerRepository.findAllById(any())).thenReturn(List.of(fantasyPlayer));

        // Execute
        FantasyGameDetailDto result = fantasyGameService.getGameDetails(gameSeq);

        // Verify
        assertNotNull(result);
        assertEquals(gameSeq, result.getSeq());
        assertEquals("2026-03-01 ~ 2026-10-30", result.getGameDuration());
        assertEquals("Team A", result.getParticipants().get(0).getTeamName());
        assertEquals(1, result.getParticipants().get(0).getRoster().size());
        assertEquals("Player 1", result.getParticipants().get(0).getRoster().get(0).getName());
    }
}
