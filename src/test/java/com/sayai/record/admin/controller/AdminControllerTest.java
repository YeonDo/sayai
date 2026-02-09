package com.sayai.record.admin.controller;

import com.sayai.record.admin.controller.AdminController;
import com.sayai.record.admin.controller.AdminController.FantasyLogDto;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.auth.service.AuthService;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyLog;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.FantasyLogRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import com.sayai.record.fantasy.service.FantasyGameService;
import com.sayai.record.fantasy.service.FantasyScoringService;
import com.sayai.record.fantasy.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class AdminControllerTest {

    @Mock private FantasyGameService fantasyGameService;
    @Mock private MemberRepository memberRepository;
    @Mock private FantasyParticipantRepository fantasyParticipantRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthService authService;
    @Mock private FantasyScoringService fantasyScoringService;
    @Mock private PostService postService;
    @Mock private FantasyLogRepository fantasyLogRepository;
    @Mock private FantasyPlayerRepository fantasyPlayerRepository;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetWaiverLogs() {
        Long gameSeq = 1L;

        // Mock Logs
        FantasyLog log = FantasyLog.builder()
                .seq(100L)
                .fantasyGameSeq(gameSeq)
                .action(FantasyLog.ActionType.DROP)
                .playerId(10L)
                .fantasyPlayerSeq(20L)
                .createdAt(LocalDateTime.now())
                .build();

        when(fantasyLogRepository.findByFantasyGameSeqAndActionOrderByCreatedAtDesc(gameSeq, FantasyLog.ActionType.DROP))
                .thenReturn(Collections.singletonList(log));

        // Mock Player
        FantasyPlayer player = FantasyPlayer.builder().seq(20L).name("PlayerName").team("TeamA").position("P").cost(10).build();
        when(fantasyPlayerRepository.findAllById(any())).thenReturn(Collections.singletonList(player));

        // Mock Participants
        FantasyParticipant participant = FantasyParticipant.builder().playerId(10L).teamName("UserTeam").build();
        when(fantasyParticipantRepository.findByFantasyGameSeq(gameSeq)).thenReturn(Collections.singletonList(participant));

        // Act
        ResponseEntity<List<FantasyLogDto>> response = adminController.getWaiverLogs(gameSeq);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        List<FantasyLogDto> body = response.getBody();
        assertEquals(1, body.size());
        assertEquals("PlayerName", body.get(0).getPlayerName());
        assertEquals("UserTeam", body.get(0).getDroppedByTeam());
    }

    @Test
    void testGetTradeRequests() {
        Long gameSeq = 1L;
        ResponseEntity<List<com.sayai.record.fantasy.dto.TradeLogDto>> response = adminController.getTradeRequests(gameSeq);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(0, response.getBody().size());
    }
}
