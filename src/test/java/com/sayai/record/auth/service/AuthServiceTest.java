package com.sayai.record.auth.service;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.jwt.JwtTokenProvider;
import com.sayai.record.auth.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        Member member = Member.builder()
                .playerId(1L)
                .username("user")
                .password("encoded_pass")
                .build();

        when(memberRepository.findByUsername("user")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("pass", "encoded_pass")).thenReturn(true);
        when(jwtTokenProvider.createToken(1L, "user")).thenReturn("jwt_token");

        String token = authService.login("user", "pass");

        assertThat(token).isEqualTo("jwt_token");
    }

    @Test
    void login_shouldThrowException_whenUserNotFound() {
        when(memberRepository.findByUsername("user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("user", "pass"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
