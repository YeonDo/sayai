package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.FantasyGameDto;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class FantasyGameServiceMyGamesTest {

    @Mock private FantasyGameRepository fantasyGameRepository;
    @Mock private FantasyParticipantRepository fantasyParticipantRepository;
    @Mock private DraftPickRepository draftPickRepository;
    @Mock private FantasyPlayerRepository fantasyPlayerRepository;
    @Mock private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @Mock private jakarta.persistence.EntityManager entityManager;
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
    void testGetMyGames_NoFilter() {
        Long userId = 100L;
        Long gameSeq1 = 1L;
        Long gameSeq2 = 2L;

        FantasyParticipant p1 = FantasyParticipant.builder().fantasyGameSeq(gameSeq1).playerId(userId).build();
        FantasyParticipant p2 = FantasyParticipant.builder().fantasyGameSeq(gameSeq2).playerId(userId).build();
        when(fantasyParticipantRepository.findByPlayerId(userId)).thenReturn(Arrays.asList(p1, p2));

        FantasyGame g1 = FantasyGame.builder().seq(gameSeq1).status(FantasyGame.GameStatus.ONGOING).title("Game1").build();
        FantasyGame g2 = FantasyGame.builder().seq(gameSeq2).status(FantasyGame.GameStatus.FINISHED).title("Game2").build();
        when(fantasyGameRepository.findAllById(Arrays.asList(gameSeq1, gameSeq2))).thenReturn(Arrays.asList(g1, g2));

        List<FantasyGameDto> result = fantasyGameService.getMyGames(userId, null);

        assertEquals(2, result.size());
    }

    @Test
    void testGetMyGames_WithFilter() {
        Long userId = 100L;
        Long gameSeq1 = 1L;
        Long gameSeq2 = 2L;

        FantasyParticipant p1 = FantasyParticipant.builder().fantasyGameSeq(gameSeq1).playerId(userId).build();
        FantasyParticipant p2 = FantasyParticipant.builder().fantasyGameSeq(gameSeq2).playerId(userId).build();
        when(fantasyParticipantRepository.findByPlayerId(userId)).thenReturn(Arrays.asList(p1, p2));

        FantasyGame g1 = FantasyGame.builder().seq(gameSeq1).status(FantasyGame.GameStatus.ONGOING).title("Game1").build();
        FantasyGame g2 = FantasyGame.builder().seq(gameSeq2).status(FantasyGame.GameStatus.FINISHED).title("Game2").build();
        when(fantasyGameRepository.findAllById(Arrays.asList(gameSeq1, gameSeq2))).thenReturn(Arrays.asList(g1, g2));

        List<FantasyGameDto> result = fantasyGameService.getMyGames(userId, FantasyGame.GameStatus.ONGOING);

        assertEquals(1, result.size());
        assertEquals(FantasyGame.GameStatus.ONGOING, result.get(0).getStatus());
        assertEquals("Game1", result.get(0).getTitle());
    }
}
