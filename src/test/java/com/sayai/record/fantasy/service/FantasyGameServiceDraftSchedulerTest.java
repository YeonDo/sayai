package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FantasyGameServiceDraftSchedulerTest {

    @Mock private FantasyGameRepository fantasyGameRepository;
    @Mock private FantasyParticipantRepository fantasyParticipantRepository;
    @Mock private DraftScheduler draftScheduler;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private FantasyGameService fantasyGameService;

    @Test
    void startGame_AddsActiveGameToScheduler() {
        Long gameSeq = 1L;
        FantasyGame game = FantasyGame.builder()
                .seq(gameSeq)
                .status(FantasyGame.GameStatus.WAITING)
                .draftTimeLimit(10)
                .build();

        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));
        when(fantasyParticipantRepository.findByFantasyGameSeq(gameSeq)).thenReturn(List.of(mock(com.sayai.record.fantasy.entity.FantasyParticipant.class)));

        fantasyGameService.startGame(gameSeq);

        verify(draftScheduler).addActiveGame(gameSeq);
    }

    @Test
    void updateGameStatus_AddsToScheduler_WhenDrafting() {
        Long gameSeq = 1L;
        FantasyGame game = FantasyGame.builder()
                .seq(gameSeq)
                .status(FantasyGame.GameStatus.WAITING)
                .build();

        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        fantasyGameService.updateGameStatus(gameSeq, FantasyGame.GameStatus.DRAFTING);

        verify(draftScheduler).addActiveGame(gameSeq);
    }

    @Test
    void updateGameStatus_RemovesFromScheduler_WhenNotDrafting() {
        Long gameSeq = 1L;
        FantasyGame game = FantasyGame.builder()
                .seq(gameSeq)
                .status(FantasyGame.GameStatus.DRAFTING)
                .build();

        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        fantasyGameService.updateGameStatus(gameSeq, FantasyGame.GameStatus.ONGOING);

        verify(draftScheduler).removeActiveGame(gameSeq);
    }
}
