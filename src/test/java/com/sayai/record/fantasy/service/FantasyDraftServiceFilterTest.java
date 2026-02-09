package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.FantasyPlayerDto;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FantasyDraftServiceFilterTest {

    @Mock
    private FantasyPlayerRepository fantasyPlayerRepository;
    @Mock
    private DraftPickRepository draftPickRepository;

    @Mock
    private com.sayai.record.fantasy.repository.FantasyLogRepository fantasyLogRepository;

    @Mock private org.springframework.beans.factory.ObjectProvider<DraftScheduler> draftSchedulerProvider;
    @Mock private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    @Mock private com.sayai.record.fantasy.repository.FantasyGameRepository fantasyGameRepository;
    @Mock private com.sayai.record.fantasy.repository.FantasyParticipantRepository fantasyParticipantRepository;
    @Mock private DraftValidator draftValidator;

    private FantasyDraftService fantasyDraftService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
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
    void getAvailablePlayers_shouldCallRepositoryWithFilters() {
        Long gameSeq = 1L;
        String team = "Doosan";
        String pos = "P";
        String search = "Kim";

        FantasyPlayer p1 = FantasyPlayer.builder().seq(1L).name("Kim").team("Doosan").position("P").cost(10).build();

        when(draftPickRepository.findByFantasyGameSeq(gameSeq)).thenReturn(Collections.emptyList());
        when(fantasyLogRepository.findByFantasyGameSeqAndActionAndIsProcessedFalseOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(gameSeq),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(Collections.emptyList());

        when(fantasyPlayerRepository.findPlayers(team, pos, search, null)).thenReturn(Collections.singletonList(p1));

        List<FantasyPlayerDto> result = fantasyDraftService.getAvailablePlayers(gameSeq, team, pos, search, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Kim");
    }

    @Test
    void getAvailablePlayers_shouldTreatEmptyStringsAsNull() {
        Long gameSeq = 0L;
        String team = "";
        String pos = "";
        String search = "";

        // Expect findPlayers to be called with nulls
        when(fantasyPlayerRepository.findPlayers(null, null, null, null)).thenReturn(Collections.emptyList());

        fantasyDraftService.getAvailablePlayers(gameSeq, team, pos, search, null, null);

        verify(fantasyPlayerRepository).findPlayers(null, null, null, null);
    }
}
