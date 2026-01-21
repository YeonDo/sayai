package com.sayai.record.auth.service;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceSignupTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void signup_shouldSaveMember_whenUserIdIsUnique() {
        String userId = "newuser";
        String password = "password";
        String name = "New User";
        Long playerId = 100L;

        when(memberRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encoded");

        authService.signup(userId, password, name, playerId);

        verify(memberRepository).save(any(Member.class));
    }

    @Test
    void signup_shouldThrowException_whenUserIdExists() {
        String userId = "existing";
        when(memberRepository.findByUserId(userId)).thenReturn(Optional.of(Member.builder().build()));

        assertThatThrownBy(() -> authService.signup(userId, "pass", "name", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID already exists");
    }
}
