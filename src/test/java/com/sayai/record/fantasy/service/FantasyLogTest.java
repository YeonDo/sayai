package com.sayai.record.fantasy.service;

import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.dto.DraftLogDto;
import com.sayai.record.fantasy.entity.FantasyLog;
import com.sayai.record.fantasy.entity.FantasyPlayer;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class FantasyLogTest {

    @Mock private FantasyGameRepository fantasyGameRepository;
    @Mock private FantasyParticipantRepository fantasyParticipantRepository;
    @Mock private DraftPickRepository draftPickRepository;
    @Mock private FantasyPlayerRepository fantasyPlayerRepository;
    @Mock private FantasyLogRepository fantasyLogRepository;
    @Mock private MemberRepository memberRepository;
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
    void testGetDraftPicks_ReturnsLogs() {
        Long gameSeq = 1L;

        // Mock Logs
        FantasyLog log1 = FantasyLog.builder().action(FantasyLog.ActionType.DRAFT).fantasyPlayerSeq(1L).playerId(100L).build();
        FantasyLog log2 = FantasyLog.builder().action(FantasyLog.ActionType.DROP).fantasyPlayerSeq(1L).playerId(100L).build();
        FantasyLog log3 = FantasyLog.builder().action(FantasyLog.ActionType.CLAIM).fantasyPlayerSeq(1L).playerId(200L).build();

        when(fantasyLogRepository.findByFantasyGameSeqOrderByCreatedAtAsc(gameSeq))
                .thenReturn(Arrays.asList(log1, log2, log3));

        // Mock Players
        FantasyPlayer p = FantasyPlayer.builder().seq(1L).name("P1").cost(10).build();
        when(fantasyPlayerRepository.findAllById(Collections.singleton(1L)))
                .thenReturn(Collections.singletonList(p));

        // Act
        List<DraftLogDto> result = fantasyGameService.getDraftPicks(gameSeq);

        // Assert
        assertEquals(3, result.size());

        // Log 1: DRAFT (Pick #1)
        assertEquals("DRAFT", result.get(0).getAction());
        assertEquals(1, result.get(0).getPickNumber());

        // Log 2: DROP
        assertEquals("DROP", result.get(1).getAction());

        // Log 3: CLAIM
        assertEquals("CLAIM", result.get(2).getAction());
    }
}
