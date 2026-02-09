package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.*;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FantasyDraftServiceAutoPickTest {

    @Mock
    private FantasyGameRepository fantasyGameRepository;
    @Mock
    private FantasyPlayerRepository fantasyPlayerRepository;
    @Mock
    private FantasyParticipantRepository fantasyParticipantRepository;
    @Mock
    private DraftPickRepository draftPickRepository;
    @Mock
    private DraftValidator draftValidator;
    @Mock
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    @Mock
    private org.springframework.beans.factory.ObjectProvider<DraftScheduler> draftSchedulerProvider;

    @InjectMocks
    private FantasyDraftService fantasyDraftService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAutoPick_FirstPickRule_Disabled() {
        // Arrange
        Long gameSeq = 1L;
        Long playerId = 100L;

        FantasyGame game = FantasyGame.builder()
                .seq(gameSeq)
                .status(FantasyGame.GameStatus.DRAFTING)
                .ruleType(FantasyGame.RuleType.RULE_2)
                .useFirstPickRule(false) // Disabled!
                .build();

        // Mock game finding
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        // Mock Picks (empty -> Round 1)
        when(draftPickRepository.countByFantasyGameSeq(gameSeq)).thenReturn(0L);
        when(draftPickRepository.findByFantasyGameSeq(gameSeq)).thenReturn(Collections.emptyList());

        // Mock Participants
        FantasyParticipant p = FantasyParticipant.builder()
                .playerId(playerId)
                .draftOrder(1)
                .preferredTeam("KIA")
                .build();
        when(fantasyParticipantRepository.findByFantasyGameSeq(gameSeq)).thenReturn(Collections.singletonList(p));
        when(fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId)).thenReturn(Optional.of(p));

        // Mock Players
        // P1: Samsung (Should be picked if rule disabled, but not if enabled and pref is KIA)
        FantasyPlayer p1 = FantasyPlayer.builder().seq(1L).name("P1").team("Samsung").cost(10).position("C").build();
        // P2: KIA
        FantasyPlayer p2 = FantasyPlayer.builder().seq(2L).name("P2").team("KIA").cost(10).position("C").build();

        List<FantasyPlayer> available = new ArrayList<>();
        available.add(p1);
        // We only add p1 to test if it CAN be picked.
        // If logic was restrictive, candidates would be empty (filtered out p1).

        when(fantasyPlayerRepository.findAll()).thenReturn(available);
        when(fantasyPlayerRepository.findById(1L)).thenReturn(Optional.of(p1));

        // Validator should pass for any player since we mock it
        doNothing().when(draftValidator).validate(any(), any(), any(), any());

        // Mock getTotalPlayerCount - required by draftPlayer logic to check if finished
        when(draftValidator.getTotalPlayerCount(any())).thenReturn(20);

        // Mock DraftPickRepository.exists to return false (not picked yet)
        when(draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(anyLong(), anyLong())).thenReturn(false);
        // Mock User Picks in draftPlayer
        when(draftPickRepository.findByFantasyGameSeqAndPlayerId(anyLong(), anyLong())).thenReturn(Collections.emptyList());

        // Act
        fantasyDraftService.autoPick(gameSeq);

        // Assert
        // Should draft P1 because rule is disabled, so candidates list wasn't filtered by team
        verify(draftPickRepository, times(1)).save(any(DraftPick.class));
    }
}
