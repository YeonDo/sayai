package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class FantasyTradeServiceTest {

    @Mock private DraftPickRepository draftPickRepository;
    @Mock private FantasyGameRepository fantasyGameRepository;
    @Mock private FantasyPlayerRepository fantasyPlayerRepository;
    @Mock private FantasyParticipantRepository fantasyParticipantRepository;

    @Mock private com.sayai.record.fantasy.repository.FantasyLogRepository fantasyLogRepository;

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
    void testClaimPlayer_Success() {
        Long gameSeq = 1L;
        Long playerId = 100L;
        Long playerSeq = 600L;

        FantasyGame game = FantasyGame.builder()
                .status(FantasyGame.GameStatus.ONGOING)
                .salaryCap(100)
                .build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        // Available
        when(draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(gameSeq, playerSeq)).thenReturn(false);

        // Player Info
        FantasyPlayer player = FantasyPlayer.builder().cost(10).build();
        when(fantasyPlayerRepository.findById(playerSeq)).thenReturn(Optional.of(player));

        // Roster Info (Size 18, Cost 50)
        when(draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, playerId)).thenReturn(18L);
        // Mock current picks cost
        DraftPick existingPick = DraftPick.builder().fantasyPlayerSeq(900L).build();
        when(draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId)).thenReturn(Collections.singletonList(existingPick));
        FantasyPlayer existingPlayer = FantasyPlayer.builder().seq(900L).cost(50).build();
        when(fantasyPlayerRepository.findAllById(any())).thenReturn(Collections.singletonList(existingPlayer));

        // Mock Participants for Order
        FantasyParticipant me = FantasyParticipant.builder().playerId(playerId).waiverOrder(1).build();
        FantasyParticipant other = FantasyParticipant.builder().playerId(101L).waiverOrder(2).build();
        List<FantasyParticipant> parts = new ArrayList<>(List.of(me, other));
        when(fantasyParticipantRepository.findByFantasyGameSeq(gameSeq)).thenReturn(parts);

        // Execute
        assertDoesNotThrow(() -> fantasyTradeService.claimPlayer(gameSeq, playerId, playerSeq));

        // Verify Order Update: Me -> Last(2), Other -> 1
        // Since list is mutable, check state or save invocation
        verify(fantasyParticipantRepository).saveAll(parts);
        // Me was 1, became 2. Other was 2, became 1.
    }

    @Test
    void testClaimPlayer_Penalty_Roster21() {
        Long gameSeq = 1L;
        Long playerId = 100L;
        Long playerSeq = 600L;

        // Cap is tight: Current 90, Cap 100.
        // Player Cost 5. New Size 21 -> Penalty +5 = Total 10. Cost 100. OK.
        FantasyGame game = FantasyGame.builder()
                .status(FantasyGame.GameStatus.ONGOING)
                .salaryCap(100)
                .build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));
        when(draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(gameSeq, playerSeq)).thenReturn(false);
        FantasyPlayer player = FantasyPlayer.builder().cost(5).build();
        when(fantasyPlayerRepository.findById(playerSeq)).thenReturn(Optional.of(player));

        // Size 20
        when(draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, playerId)).thenReturn(20L);

        // Mock current cost 90
        DraftPick existingPick = DraftPick.builder().fantasyPlayerSeq(900L).build();
        when(draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId)).thenReturn(Collections.singletonList(existingPick));
        FantasyPlayer existingPlayer = FantasyPlayer.builder().seq(900L).cost(90).build();
        when(fantasyPlayerRepository.findAllById(any())).thenReturn(Collections.singletonList(existingPlayer));

        // Participants
        FantasyParticipant me = FantasyParticipant.builder().playerId(playerId).waiverOrder(1).build();
        when(fantasyParticipantRepository.findByFantasyGameSeq(gameSeq)).thenReturn(new ArrayList<>(List.of(me)));

        assertDoesNotThrow(() -> fantasyTradeService.claimPlayer(gameSeq, playerId, playerSeq));
    }

    @Test
    void testClaimPlayer_Fail_RosterFull() {
        Long gameSeq = 1L;
        Long playerId = 100L;
        Long playerSeq = 600L;

        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));
        when(draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(gameSeq, playerSeq)).thenReturn(false);
        // Add cost to builder to avoid NPE in service logic if checked earlier or later
        when(fantasyPlayerRepository.findById(playerSeq)).thenReturn(Optional.of(FantasyPlayer.builder().cost(10).build()));

        // Size 21
        when(draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, playerId)).thenReturn(21L);

        assertThrows(IllegalStateException.class, () -> fantasyTradeService.claimPlayer(gameSeq, playerId, playerSeq));
    }

    @Test
    void testClaimPlayer_Fail_OverCap() {
        Long gameSeq = 1L;
        Long playerId = 100L;
        Long playerSeq = 600L;

        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).salaryCap(100).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));
        when(draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(gameSeq, playerSeq)).thenReturn(false);
        FantasyPlayer player = FantasyPlayer.builder().cost(20).build(); // 90 + 20 = 110 > 100
        when(fantasyPlayerRepository.findById(playerSeq)).thenReturn(Optional.of(player));

        when(draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, playerId)).thenReturn(18L);
        DraftPick existingPick = DraftPick.builder().fantasyPlayerSeq(900L).build();
        when(draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId)).thenReturn(Collections.singletonList(existingPick));
        FantasyPlayer existingPlayer = FantasyPlayer.builder().seq(900L).cost(90).build();
        when(fantasyPlayerRepository.findAllById(any())).thenReturn(Collections.singletonList(existingPlayer));

        // Mock participants for order logic which runs before saving, just in case
        FantasyParticipant me = FantasyParticipant.builder().playerId(playerId).waiverOrder(1).build();
        when(fantasyParticipantRepository.findByFantasyGameSeq(gameSeq)).thenReturn(new ArrayList<>(List.of(me)));

        assertThrows(IllegalStateException.class, () -> fantasyTradeService.claimPlayer(gameSeq, playerId, playerSeq));
    }
}
