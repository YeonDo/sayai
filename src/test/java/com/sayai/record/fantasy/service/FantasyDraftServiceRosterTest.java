package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.RosterUpdateDto;
import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FantasyDraftServiceRosterTest {

    @Mock
    private FantasyGameRepository fantasyGameRepository;
    @Mock
    private DraftPickRepository draftPickRepository;

    @InjectMocks
    private FantasyDraftService fantasyDraftService;

    @Test
    void updateRoster_shouldAllowMultiplePitchers() {
        Long gameSeq = 1L;
        Long playerId = 100L;
        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).build();

        // Existing picks: SP, SP (total 2 SPs)
        DraftPick pick1 = DraftPick.builder().fantasyPlayerSeq(1L).playerId(playerId).assignedPosition("SP").build();
        DraftPick pick2 = DraftPick.builder().fantasyPlayerSeq(2L).playerId(playerId).assignedPosition("SP").build();
        // New assignment: another SP (total 3 SPs -> allowed, limit 4)
        DraftPick pick3 = DraftPick.builder().fantasyPlayerSeq(3L).playerId(playerId).assignedPosition(null).build(); // Unassigned initially

        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));
        when(draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId))
                .thenReturn(List.of(pick1, pick2, pick3));

        RosterUpdateDto dto = new RosterUpdateDto();
        dto.setEntries(List.of(new RosterUpdateDto.RosterEntry(3L, "SP")));

        assertThatCode(() -> fantasyDraftService.updateRoster(gameSeq, playerId, dto))
                .doesNotThrowAnyException();
    }

    @Test
    void updateRoster_shouldThrow_whenLimitExceeded() {
        Long gameSeq = 1L;
        Long playerId = 100L;
        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).build();

        // Existing picks: 4 SPs already
        DraftPick pick1 = DraftPick.builder().fantasyPlayerSeq(1L).playerId(playerId).assignedPosition("SP").build();
        DraftPick pick2 = DraftPick.builder().fantasyPlayerSeq(2L).playerId(playerId).assignedPosition("SP").build();
        DraftPick pick3 = DraftPick.builder().fantasyPlayerSeq(3L).playerId(playerId).assignedPosition("SP").build();
        DraftPick pick4 = DraftPick.builder().fantasyPlayerSeq(4L).playerId(playerId).assignedPosition("SP").build();

        // Attempt to set 5th SP
        DraftPick pick5 = DraftPick.builder().fantasyPlayerSeq(5L).playerId(playerId).assignedPosition(null).build();

        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));
        when(draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId))
                .thenReturn(List.of(pick1, pick2, pick3, pick4, pick5));

        RosterUpdateDto dto = new RosterUpdateDto();
        dto.setEntries(List.of(new RosterUpdateDto.RosterEntry(5L, "SP")));

        assertThatThrownBy(() -> fantasyDraftService.updateRoster(gameSeq, playerId, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Position limit exceeded");
    }
}
