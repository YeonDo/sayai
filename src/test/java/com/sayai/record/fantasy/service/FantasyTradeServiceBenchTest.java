package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.TradeProposalDto;
import com.sayai.record.fantasy.entity.*;
import com.sayai.record.fantasy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FantasyTradeServiceBenchTest {

    @Mock private DraftPickRepository draftPickRepository;
    @Mock private FantasyGameRepository fantasyGameRepository;
    @Mock private FantasyPlayerRepository fantasyPlayerRepository;
    @Mock private FantasyParticipantRepository fantasyParticipantRepository;
    @Mock private FantasyLogRepository fantasyLogRepository;
    @Mock private FantasyTradeRepository fantasyTradeRepository;
    @Mock private FantasyTradePlayerRepository fantasyTradePlayerRepository;

    @InjectMocks
    private FantasyTradeService fantasyTradeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDropPlayer_BenchVariant_Success() {
        Long gameSeq = 1L;
        Long playerId = 100L;
        Long playerSeq = 500L;

        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        when(draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, playerId)).thenReturn(20L);

        // BENCH-1
        DraftPick pick = DraftPick.builder().assignedPosition("BENCH-1").build();
        when(draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(gameSeq, playerId, playerSeq))
                .thenReturn(Optional.of(pick));

        assertDoesNotThrow(() -> fantasyTradeService.dropPlayer(gameSeq, playerId, playerSeq));
    }

    @Test
    void testProposeTrade_BenchVariant_Success() {
        Long gameSeq = 1L;
        Long proposerId = 100L;
        Long targetId = 200L;

        TradeProposalDto dto = new TradeProposalDto();
        dto.setGameSeq(gameSeq);
        dto.setTargetPlayerId(targetId);
        dto.setMyPlayers(List.of(10L));
        dto.setTargetPlayers(List.of(20L));

        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        FantasyTrade trade = FantasyTrade.builder().seq(1L).build();
        when(fantasyTradeRepository.save(any(FantasyTrade.class))).thenReturn(trade);

        // My Player: BENCH-2
        DraftPick myPick = DraftPick.builder().assignedPosition("BENCH-2").build();
        when(draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(gameSeq, proposerId, 10L))
                .thenReturn(Optional.of(myPick));

        // Target Player: BENCH
        DraftPick targetPick = DraftPick.builder().assignedPosition("BENCH").build();
        when(draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(gameSeq, targetId, 20L))
                .thenReturn(Optional.of(targetPick));

        assertDoesNotThrow(() -> fantasyTradeService.proposeTrade(proposerId, dto));
    }
}
