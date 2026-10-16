package com.sayai.record.admin.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.service.FantasyGameService;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private FantasyGameService fantasyGameService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.sayai.record.fantasy.repository.FantasyPlayerRepository fantasyPlayerRepository;

    @Mock
    private com.sayai.record.point.PointService pointService;

    @InjectMocks
    private AdminController adminController;

    @Test
    void createGame_shouldSaveGame() {
        AdminController.GameCreateRequest req = new AdminController.GameCreateRequest();
        req.setTitle("New League");
        req.setRuleType(FantasyGame.RuleType.RULE_1);
        req.setScoringType(FantasyGame.ScoringType.POINTS);

        when(fantasyGameService.createGame(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
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
                .memberId(1L)
                .userId("testuser")
                .name("Test User")
                .role(Member.Role.USER)
                .password("encodedPwd")
                .build();

        when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(member)));

        ResponseEntity<org.springframework.data.domain.Page<AdminController.MemberDto>> response =
                adminController.listUsers(0, 20);

        assertThat(response.getBody().getContent()).hasSize(1);
        AdminController.MemberDto dto = response.getBody().getContent().get(0);
        assertThat(dto.getMemberId()).isEqualTo(1L);
        assertThat(dto.getUserId()).isEqualTo("testuser");
        assertThat(dto.getName()).isEqualTo("Test User");
        assertThat(dto.getRole()).isEqualTo(Member.Role.USER);
    }

    @Test
    void createUser_shouldReturnBadRequest_whenValidationFails() {
        AdminController.UserCreateRequest req = new AdminController.UserCreateRequest();
        // req has null fields

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getAllErrors()).thenReturn(List.of(new ObjectError("userCreateRequest", "Player ID is required")));

        ResponseEntity<String> response = adminController.createUser(req, bindingResult);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).contains("Player ID is required");
        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void createUser_shouldCreateUser_whenValidationPasses() {
        AdminController.UserCreateRequest req = new AdminController.UserCreateRequest();
        req.setMemberId(10L);
        req.setUserId("newUser");
        req.setName("New User");
        req.setPassword("pass");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        when(memberRepository.existsByMemberIdOrUserId(10L, "newUser")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");

        ResponseEntity<String> response = adminController.createUser(req, bindingResult);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("User created");
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    void createFantasyPlayer_shouldRejectMissingRequiredFields() {
        AdminController.FantasyPlayerCreateRequest req = new AdminController.FantasyPlayerCreateRequest();
        req.setName("홍길동"); // position, team 누락

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adminController.createFantasyPlayer(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
        verify(fantasyPlayerRepository, never()).save(any());
    }

    @Test
    void createFantasyPlayer_shouldCreateWithMaxSeqPlusOneAndDefaults() {
        AdminController.FantasyPlayerCreateRequest req = new AdminController.FantasyPlayerCreateRequest();
        req.setName("홍길동");
        req.setPosition("SS");
        req.setTeam("KIA");
        // cost, foreignerType 미지정 → 0 / NONE

        when(fantasyPlayerRepository.findMaxSeq()).thenReturn(100L);

        ResponseEntity<AdminController.FantasyPlayerDto> response = adminController.createFantasyPlayer(req);

        AdminController.FantasyPlayerDto dto = response.getBody();
        assertThat(dto.getSeq()).isEqualTo(101L);
        assertThat(dto.getName()).isEqualTo("홍길동");
        assertThat(dto.getPosition()).isEqualTo("SS");
        assertThat(dto.getTeam()).isEqualTo("KIA");
        assertThat(dto.getCost()).isEqualTo(0);
        assertThat(dto.getForeignerType()).isEqualTo(com.sayai.record.fantasy.entity.FantasyPlayer.ForeignerType.NONE);
        assertThat(dto.getIsActive()).isEqualTo(0);
        verify(fantasyPlayerRepository).save(any(com.sayai.record.fantasy.entity.FantasyPlayer.class));
    }
}
