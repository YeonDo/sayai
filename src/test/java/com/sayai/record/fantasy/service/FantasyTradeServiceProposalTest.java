package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.TradeProposalDto;
import com.sayai.record.fantasy.entity.*;
import com.sayai.record.fantasy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class FantasyTradeServiceProposalTest {

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
    void testProposeTrade_Success() {
        Long gameSeq = 1L;
        Long proposerId = 100L;
        Long targetId = 200L;

        TradeProposalDto dto = new TradeProposalDto();
        dto.setGameSeq(gameSeq);
        dto.setTargetPlayerId(targetId);
        dto.setMyPlayers(List.of(10L));
        dto.setTargetPlayers(List.of(20L));

        FantasyTrade trade = FantasyTrade.builder().seq(1L).build();
        when(fantasyTradeRepository.save(any(FantasyTrade.class))).thenReturn(trade);

        // Mock ownership and bench status
        DraftPick myPick = DraftPick.builder().assignedPosition("BENCH").build();
        when(draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(gameSeq, proposerId, 10L))
                .thenReturn(Optional.of(myPick));

        DraftPick targetPick = DraftPick.builder().assignedPosition("BENCH").build();
        when(draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(gameSeq, targetId, 20L))
                .thenReturn(Optional.of(targetPick));

        assertDoesNotThrow(() -> fantasyTradeService.proposeTrade(proposerId, dto));
        verify(fantasyTradePlayerRepository, times(2)).save(any(FantasyTradePlayer.class));
    }

    @Test
    void testApproveTrade_Success() {
        Long tradeSeq = 1L;
        Long gameSeq = 10L;
        Long proposerId = 100L;
        Long targetId = 200L;

        FantasyTrade trade = FantasyTrade.builder()
                .seq(tradeSeq).fantasyGameSeq(gameSeq)
                .proposerId(proposerId).targetId(targetId)
                .status(FantasyTrade.TradeStatus.PROPOSED)
                .build();

        when(fantasyTradeRepository.findById(tradeSeq)).thenReturn(Optional.of(trade));

        FantasyGame game = FantasyGame.builder().seq(gameSeq).salaryCap(1000).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        // Trade Players: 1 for 1
        FantasyTradePlayer tp1 = FantasyTradePlayer.builder().playerId(proposerId).fantasyPlayerSeq(10L).build();
        FantasyTradePlayer tp2 = FantasyTradePlayer.builder().playerId(targetId).fantasyPlayerSeq(20L).build();
        when(fantasyTradePlayerRepository.findByFantasyTradeSeq(tradeSeq)).thenReturn(Arrays.asList(tp1, tp2));

        // Mock Costs
        // P1 cost 10, P2 cost 15
        FantasyPlayer p1 = FantasyPlayer.builder().seq(10L).cost(10).build();
        FantasyPlayer p2 = FantasyPlayer.builder().seq(20L).cost(15).build();
        when(fantasyPlayerRepository.findAllById(any())).thenReturn(Arrays.asList(p1, p2));

        // Mock Picks needed for execution
        DraftPick pick1 = DraftPick.builder().playerId(proposerId).fantasyPlayerSeq(10L).assignedPosition("BENCH").build();
        DraftPick pick2 = DraftPick.builder().playerId(targetId).fantasyPlayerSeq(20L).assignedPosition("BENCH").build();

        // Simulating DB finds
        when(draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(gameSeq, proposerId, 10L)).thenReturn(Optional.of(pick1));
        when(draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(gameSeq, targetId, 20L)).thenReturn(Optional.of(pick2));

        // Mock validation list (Proposer has P1, Target has P2)
        // Proposer Roster Cost check: removing P1, adding P2.
        when(draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, proposerId)).thenReturn(Collections.singletonList(pick1));
        when(draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, targetId)).thenReturn(Collections.singletonList(pick2));

        assertDoesNotThrow(() -> fantasyTradeService.approveTrade(tradeSeq));

        // Verify execution
        verify(draftPickRepository, times(2)).save(any(DraftPick.class)); // 2 updates
        verify(fantasyLogRepository, times(2)).save(any(FantasyLog.class)); // 2 logs
        verify(fantasyTradeRepository, times(1)).save(trade); // Status update
    }
}
