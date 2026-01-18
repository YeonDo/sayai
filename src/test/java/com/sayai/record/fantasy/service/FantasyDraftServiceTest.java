package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.DraftRequest;
import com.sayai.record.fantasy.dto.FantasyPlayerDto;
import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FantasyDraftServiceTest {

    @Mock
    private FantasyPlayerRepository fantasyPlayerRepository;
    @Mock
    private DraftPickRepository draftPickRepository;
    @Mock
    private FantasyGameRepository fantasyGameRepository;

    @InjectMocks
    private FantasyDraftService fantasyDraftService;

    @Test
    void getAvailablePlayers_shouldFilterPickedPlayers() {
        // Given
        Long gameSeq = 1L;
        FantasyPlayer p1 = FantasyPlayer.builder().seq(1L).name("P1").build();
        FantasyPlayer p2 = FantasyPlayer.builder().seq(2L).name("P2").build();

        when(draftPickRepository.findByFantasyGameSeq(gameSeq))
                .thenReturn(Collections.singletonList(
                        DraftPick.builder().fantasyPlayerSeq(1L).build()
                ));
        when(fantasyPlayerRepository.findAll()).thenReturn(Arrays.asList(p1, p2));

        // When
        List<FantasyPlayerDto> result = fantasyDraftService.getAvailablePlayers(gameSeq);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("P2");
    }

    @Test
    void draftPlayer_shouldFail_whenGameNotDrafting() {
        DraftRequest req = DraftRequest.builder().fantasyGameSeq(1L).build();
        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.WAITING).build();

        when(fantasyGameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> fantasyDraftService.draftPlayer(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Game is not in DRAFTING status");
    }

    @Test
    void draftPlayer_shouldFail_whenAlreadyPicked() {
        DraftRequest req = DraftRequest.builder().fantasyGameSeq(1L).fantasyPlayerSeq(10L).build();
        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.DRAFTING).build();

        when(fantasyGameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> fantasyDraftService.draftPlayer(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Player already picked");
    }

    @Test
    void draftPlayer_shouldSucceed() {
        DraftRequest req = DraftRequest.builder()
                .fantasyGameSeq(1L)
                .fantasyPlayerSeq(10L)
                .playerId(100L)
                .build();
        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.DRAFTING).build();

        when(fantasyGameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(1L, 10L)).thenReturn(false);

        fantasyDraftService.draftPlayer(req);

        verify(draftPickRepository).save(any(DraftPick.class));
    }
}
