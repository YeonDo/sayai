package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.*;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyLogRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class FantasyTradeServiceAssignTest {

    @Mock private DraftPickRepository draftPickRepository;
    @Mock private FantasyGameRepository fantasyGameRepository;
    @Mock private FantasyPlayerRepository fantasyPlayerRepository;
    @Mock private FantasyParticipantRepository fantasyParticipantRepository;
    @Mock private FantasyLogRepository fantasyLogRepository;

    @InjectMocks
    private FantasyTradeService fantasyTradeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAssignPlayerByAdmin_Success() {
        Long gameSeq = 1L;
        Long targetPlayerId = 100L;
        Long playerSeq = 500L;

        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        when(draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(gameSeq, playerSeq)).thenReturn(false);
        when(fantasyPlayerRepository.findById(playerSeq)).thenReturn(Optional.of(FantasyPlayer.builder().cost(10).build()));

        when(draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, targetPlayerId)).thenReturn(18L);
        when(draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, targetPlayerId)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> fantasyTradeService.assignPlayerByAdmin(gameSeq, targetPlayerId, playerSeq, null));
        verify(draftPickRepository, times(1)).save(any(DraftPick.class));
        verify(fantasyLogRepository, times(1)).save(any(FantasyLog.class));
    }

    @Test
    void testAssignPlayerByAdmin_Fail_RosterFull() {
        Long gameSeq = 1L;
        Long targetPlayerId = 100L;
        Long playerSeq = 500L;

        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));
        when(draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(gameSeq, playerSeq)).thenReturn(false);
        when(fantasyPlayerRepository.findById(playerSeq)).thenReturn(Optional.of(FantasyPlayer.builder().cost(10).build()));

        // Size 21 -> Full
        when(draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, targetPlayerId)).thenReturn(21L);

        assertThrows(IllegalStateException.class, () -> fantasyTradeService.assignPlayerByAdmin(gameSeq, targetPlayerId, playerSeq, null));
    }

    @Test
    void testAssignPlayerByAdmin_Fail_SalaryCap() {
        Long gameSeq = 1L;
        Long targetPlayerId = 100L;
        Long playerSeq = 500L;

        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).salaryCap(100).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));
        when(draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(gameSeq, playerSeq)).thenReturn(false);

        FantasyPlayer player = FantasyPlayer.builder().cost(20).build();
        when(fantasyPlayerRepository.findById(playerSeq)).thenReturn(Optional.of(player));

        when(draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, targetPlayerId)).thenReturn(18L);

        // Mock current cost 90
        DraftPick existingPick = DraftPick.builder().fantasyPlayerSeq(900L).build();
        when(draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, targetPlayerId)).thenReturn(Collections.singletonList(existingPick));
        FantasyPlayer existingPlayer = FantasyPlayer.builder().seq(900L).cost(90).build();
        when(fantasyPlayerRepository.findAllById(any())).thenReturn(Collections.singletonList(existingPlayer));

        // 90 + 20 = 110 > 100
        assertThrows(IllegalStateException.class, () -> fantasyTradeService.assignPlayerByAdmin(gameSeq, targetPlayerId, playerSeq, null));
    }
}
