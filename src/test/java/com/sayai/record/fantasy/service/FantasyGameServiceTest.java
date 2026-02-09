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
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class FantasyGameServiceTest {

    @Mock private FantasyGameRepository fantasyGameRepository;
    @Mock private FantasyParticipantRepository fantasyParticipantRepository;
    @Mock private DraftPickRepository draftPickRepository;
    @Mock private FantasyPlayerRepository fantasyPlayerRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private FantasyDraftService fantasyDraftService;
    @Mock private DraftScheduler draftScheduler;

    @InjectMocks
    private FantasyGameService fantasyGameService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testExportRoster_ExcludesBench() {
        Long gameSeq = 1L;
        FantasyGame game = FantasyGame.builder().seq(gameSeq).status(FantasyGame.GameStatus.ONGOING).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        FantasyParticipant participant = FantasyParticipant.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(100L)
                .teamName("MyTeam")
                .build();
        when(fantasyParticipantRepository.findByFantasyGameSeq(gameSeq)).thenReturn(Collections.singletonList(participant));

        FantasyPlayer p1 = FantasyPlayer.builder().seq(1L).name("Player1").position("C").cost(10).build();
        FantasyPlayer p2 = FantasyPlayer.builder().seq(2L).name("Player2").position("1B").cost(10).build(); // Bench by explicit
        FantasyPlayer p3 = FantasyPlayer.builder().seq(3L).name("Player3").position("2B").cost(10).build(); // Bench by null

        when(fantasyPlayerRepository.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(Arrays.asList(p1, p2, p3));

        DraftPick pick1 = DraftPick.builder().fantasyGameSeq(gameSeq).playerId(100L).fantasyPlayerSeq(1L).assignedPosition("C").build();
        DraftPick pick2 = DraftPick.builder().fantasyGameSeq(gameSeq).playerId(100L).fantasyPlayerSeq(2L).assignedPosition("BENCH").build();
        DraftPick pick3 = DraftPick.builder().fantasyGameSeq(gameSeq).playerId(100L).fantasyPlayerSeq(3L).assignedPosition(null).build();

        when(draftPickRepository.findByFantasyGameSeq(gameSeq)).thenReturn(Arrays.asList(pick1, pick2, pick3));

        String result = fantasyGameService.exportRoster(gameSeq);

        assertTrue(result.contains("Player1"));
        assertFalse(result.contains("Player2")); // BENCH
        assertFalse(result.contains("Player3")); // null
    }
}
