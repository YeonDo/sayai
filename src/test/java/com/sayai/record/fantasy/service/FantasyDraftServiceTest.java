package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.FantasyPlayerDto;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class FantasyDraftServiceTest {

    @Mock private FantasyPlayerRepository fantasyPlayerRepository;
    @Mock private DraftPickRepository draftPickRepository;
    @Mock private FantasyGameRepository fantasyGameRepository;
    @Mock private FantasyParticipantRepository fantasyParticipantRepository;
    @Mock private DraftValidator draftValidator;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private com.sayai.record.fantasy.repository.FantasyLogRepository fantasyLogRepository;
    @Mock private org.springframework.beans.factory.ObjectProvider<DraftScheduler> draftSchedulerProvider;

    private FantasyDraftService fantasyDraftService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fantasyDraftService = new FantasyDraftService(
            fantasyPlayerRepository,
            draftPickRepository,
            fantasyGameRepository,
            fantasyParticipantRepository,
            fantasyLogRepository,
            draftValidator,
            messagingTemplate,
            draftSchedulerProvider
        );
    }

    @Test
    void testGetAvailablePlayers_ForeignerType() {
        // Arrange
        FantasyPlayer p1 = FantasyPlayer.builder().seq(1L).name("P1").foreignerType(FantasyPlayer.ForeignerType.TYPE_1).cost(10).build();
        FantasyPlayer p2 = FantasyPlayer.builder().seq(2L).name("P2").foreignerType(FantasyPlayer.ForeignerType.NONE).cost(10).build();

        // When filtering by TYPE_1, repository returns p1
        when(fantasyPlayerRepository.findPlayers(isNull(), isNull(), isNull(), eq(FantasyPlayer.ForeignerType.TYPE_1)))
                .thenReturn(Collections.singletonList(p1));

        // When filtering by NONE, repository returns p2
        when(fantasyPlayerRepository.findPlayers(isNull(), isNull(), isNull(), eq(FantasyPlayer.ForeignerType.NONE)))
                .thenReturn(Collections.singletonList(p2));

        // Mock DraftPicks (empty)
        when(draftPickRepository.findByFantasyGameSeq(anyLong())).thenReturn(Collections.emptyList());
        when(fantasyLogRepository.findByFantasyGameSeqAndActionAndIsProcessedFalseOrderByCreatedAtDesc(anyLong(), any())).thenReturn(Collections.emptyList());

        // Act & Assert 1
        List<FantasyPlayerDto> result1 = fantasyDraftService.getAvailablePlayers(1L, null, null, null, null, "TYPE_1");
        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("P1", result1.get(0).getName());

        // Act & Assert 2
        List<FantasyPlayerDto> result2 = fantasyDraftService.getAvailablePlayers(1L, null, null, null, null, "NONE");
        assertNotNull(result2);
        assertEquals(1, result2.size());
        assertEquals("P2", result2.get(0).getName());
    }
}
