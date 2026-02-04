package com.sayai.record.admin.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.service.FantasyGameService;
import org.junit.jupiter.api.Test;

import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private FantasyGameService fantasyGameService;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AdminController adminController;

    @Test
    void createGame_shouldSaveGame() {
        AdminController.GameCreateRequest req = new AdminController.GameCreateRequest();
        req.setTitle("New League");
        req.setRuleType(FantasyGame.RuleType.RULE_1);
        req.setScoringType(FantasyGame.ScoringType.POINTS);

        when(fantasyGameService.createGame(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(FantasyGame.builder()
                .title("New League")
                .scoringType(FantasyGame.ScoringType.POINTS)
                .build());

        ResponseEntity<FantasyGame> response = adminController.createGame(req);

        assertThat(response.getBody().getTitle()).isEqualTo("New League");
        assertThat(response.getBody().getScoringType()).isEqualTo(FantasyGame.ScoringType.POINTS);
    }

    @Test
    void listUsers_shouldReturnMemberDtos() {
        Member member = Member.builder()
                .playerId(1L)
                .userId("testuser")
                .name("Test User")
                .role(Member.Role.USER)
                .password("encodedPwd")
                .build();

        when(memberRepository.findAll()).thenReturn(List.of(member));

        ResponseEntity<List<AdminController.MemberDto>> response = adminController.listUsers();

        assertThat(response.getBody()).hasSize(1);
        AdminController.MemberDto dto = response.getBody().get(0);
        assertThat(dto.getPlayerId()).isEqualTo(1L);
        assertThat(dto.getUserId()).isEqualTo("testuser");
        assertThat(dto.getName()).isEqualTo("Test User");
        assertThat(dto.getRole()).isEqualTo(Member.Role.USER);
    }
}
