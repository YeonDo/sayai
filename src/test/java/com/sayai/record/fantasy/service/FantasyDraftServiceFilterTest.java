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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FantasyDraftServiceFilterTest {

    @Mock
    private FantasyPlayerRepository fantasyPlayerRepository;
    @Mock
    private DraftPickRepository draftPickRepository;

    @InjectMocks
    private FantasyDraftService fantasyDraftService;

    @Test
    void getAvailablePlayers_shouldCallRepositoryWithFilters() {
        Long gameSeq = 1L;
        String team = "Doosan";
        String pos = "P";
        String search = "Kim";

        FantasyPlayer p1 = FantasyPlayer.builder().seq(1L).name("Kim").team("Doosan").position("P").build();

        when(draftPickRepository.findByFantasyGameSeq(gameSeq)).thenReturn(Collections.emptyList());
        when(fantasyPlayerRepository.findPlayers(team, pos, search)).thenReturn(Collections.singletonList(p1));

        List<FantasyPlayerDto> result = fantasyDraftService.getAvailablePlayers(gameSeq, team, pos, search, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Kim");
    }
}
