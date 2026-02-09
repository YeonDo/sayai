package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class FantasyTradeServiceTest {

    @Mock
    private DraftPickRepository draftPickRepository;
    @Mock
    private FantasyGameRepository fantasyGameRepository;

    @InjectMocks
    private FantasyTradeService fantasyTradeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDropPlayer_Success() {
        Long gameSeq = 1L;
        Long playerId = 100L;
        Long playerSeq = 500L;

        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        when(draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, playerId)).thenReturn(20L);

        DraftPick pick = DraftPick.builder().assignedPosition("BENCH").build();
        when(draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(gameSeq, playerId, playerSeq))
                .thenReturn(Optional.of(pick));

        assertDoesNotThrow(() -> fantasyTradeService.dropPlayer(gameSeq, playerId, playerSeq));
        verify(draftPickRepository, times(1)).delete(pick);
    }

    @Test
    void testDropPlayer_Fail_RosterSize() {
        Long gameSeq = 1L;
        Long playerId = 100L;
        Long playerSeq = 500L;

        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        // Roster size 18 -> Cannot drop
        when(draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, playerId)).thenReturn(18L);

        assertThrows(IllegalStateException.class, () -> fantasyTradeService.dropPlayer(gameSeq, playerId, playerSeq));
    }

    @Test
    void testDropPlayer_Success_RosterSize19() {
        Long gameSeq = 1L;
        Long playerId = 100L;
        Long playerSeq = 500L;

        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        // Roster size 19 -> Can drop
        when(draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, playerId)).thenReturn(19L);

        DraftPick pick = DraftPick.builder().assignedPosition("BENCH").build();
        when(draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(gameSeq, playerId, playerSeq))
                .thenReturn(Optional.of(pick));

        assertDoesNotThrow(() -> fantasyTradeService.dropPlayer(gameSeq, playerId, playerSeq));
    }

    @Test
    void testDropPlayer_Fail_NotBench() {
        Long gameSeq = 1L;
        Long playerId = 100L;
        Long playerSeq = 500L;

        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        when(draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, playerId)).thenReturn(20L);

        DraftPick pick = DraftPick.builder().assignedPosition("C").build(); // Starter
        when(draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(gameSeq, playerId, playerSeq))
                .thenReturn(Optional.of(pick));

        assertThrows(IllegalStateException.class, () -> fantasyTradeService.dropPlayer(gameSeq, playerId, playerSeq));
    }
}
